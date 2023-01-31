package com.flowmsp.service.pubsub;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

public class googleauthorization {
	private static final Logger log = LoggerFactory.getLogger(googleauthorization.class);
	private final googlecredentials credential;
	public googleaccesstoken accesstoken;
	
	public googleauthorization (googlecredentials credential) {
		this.credential = credential;		
	}
	
	public int RefreshAccessToken() {
		this.accesstoken = new googleaccesstoken();
		this.accesstoken.topicName = this.credential.topicName;
		//Generate Access Token
		// TODO: Get this from config
		String targetURL = "https://www.googleapis.com/oauth2/v4/token";
		HttpURLConnection connection = null;
		log.info("Refreshing Access Token");
		try {
				String urlParameters = "client_id=" + this.credential.clientID +
						"&client_secret=" + this.credential.clientSecret + 
						"&refresh_token=" + this.credential.refreshToken +
						"&grant_type=refresh_token";
				byte[] postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
				int postDataLength = postData.length;
				// Create connection
				URL url = new URL(targetURL);				
				connection = (HttpURLConnection) url.openConnection();
				connection.setDoOutput(true);
				connection.setInstanceFollowRedirects(false);
				connection.setRequestMethod("POST");
				connection.setReadTimeout(30000); // set the connection timeout value to 30 seconds (30000 milliseconds)
				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
				connection.setRequestProperty("charset", "utf-8");
				connection.setRequestProperty("Content-Length", Integer.toString(postDataLength ));
				connection.setUseCaches(false);
				try(DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
				   wr.write( postData );
				   wr.flush();
				}
				
				int responseCode = connection.getResponseCode();
				if (responseCode != 200) {
					// 200 is for HTTP 200 OK
					log.error("Error timeout by google api");
					return -1;
				}
				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
			
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				// now parse
				JSONParser parser = new JSONParser();
				Object obj = parser.parse(response.toString());
				JSONObject jb = (JSONObject) obj;
				this.accesstoken.accessToken = jb.get("access_token").toString();	
				this.accesstoken.accessTokenExpirySeconds = (int) jb.get("expires_in");
				
				log.info("Access Token Aquired:" + this.accesstoken.accessToken);
				
				Calendar calendar = Calendar.getInstance();
				this.accesstoken.accessTokenTimeStamp = calendar.getTime();				
				calendar.setTime(this.accesstoken.accessTokenTimeStamp);
				calendar.add(Calendar.SECOND, this.accesstoken.accessTokenExpirySeconds);
				this.accesstoken.accessTokenExpiryTimeStamp = calendar.getTime();
				log.info("Successfully Refreshed Token");
				return 0;
			} catch (Exception e) {
				log.error("Error Refreshing the Access Token", e);
				return -2;
			} finally {
				if (connection != null) {
					connection.disconnect();
			}
		}
	}
}
