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

public class pubsubhttp {
	private static final Logger log = LoggerFactory.getLogger(pubsubhttp.class);
	private static pubsubhttp myInstance = null;
	
	private pubsubhttp() {
		
	}

	public static pubsubhttp GetMyInstance() {
		if (myInstance == null) {
			myInstance = new pubsubhttp();
		}
		return myInstance;
	}	
	
	public void RefreshTokenIfExpired() {
		if (googlepubsub.GetMyInstance().authorization.accesstoken.accessToken == null) {
			googlepubsub.GetMyInstance().authorization.RefreshAccessToken();
		}
		else if(Calendar.getInstance().getTime().after(googlepubsub.GetMyInstance().authorization.accesstoken.accessTokenExpiryTimeStamp)) {
			googlepubsub.GetMyInstance().authorization.RefreshAccessToken();
		}
	}
	
	public JSONObject PerfromGET(String targetURL) {
		RefreshTokenIfExpired();
		HttpURLConnection connection = null;
		try {
			String accessToken = googlepubsub.GetMyInstance().authorization.accesstoken.accessToken;
			//log.info("HTTP Get With Access Token:" + accessToken + " URL:" + targetURL);
			// Create connection
			URL url = new URL(targetURL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("GET");
			connection.setReadTimeout(60000); // set the connection timeout value to 60 seconds (60000 milliseconds)
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Authorization", "Bearer " + accessToken);
			connection.setUseCaches(false);
			int responseCode = connection.getResponseCode();
			if (responseCode != 200) {
				// 200 is for HTTP 200 OK
				log.error("Error timeout by google api:" + responseCode);
				return null;
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
			return jb;
		} catch (Exception e) {
			log.error("Error performing HTTP Get:" + targetURL, e);
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	public JSONObject PerformPOST(String targetURL,  String urlParameters) {
		RefreshTokenIfExpired();
		HttpURLConnection connection = null;
		try {
				String accessToken = googlepubsub.GetMyInstance().authorization.accesstoken.accessToken;
				log.info("HTTP POST With Access Token:" + accessToken + " URL:" + targetURL);
				// Create connection
				URL url = new URL(targetURL);
				connection = (HttpURLConnection) url.openConnection();
				byte[] postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
				int postDataLength = postData.length;
				connection.setDoOutput(true);
				connection.setInstanceFollowRedirects(false);
				connection.setRequestMethod("POST");
				connection.setReadTimeout(30000); // set the connection timeout value to 30 seconds (30000 milliseconds)
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestProperty("charset", "utf-8");
				connection.setRequestProperty("Content-Length", Integer.toString(postDataLength ));
				connection.setRequestProperty("Authorization", "Bearer " + accessToken);
				connection.setUseCaches(false);
				try(DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
				   wr.write( postData );
				   wr.flush();
				}
				
				int responseCode = connection.getResponseCode();
				if (responseCode != 200) {
					// 200 is for HTTP 200 OK
					log.error("responseCode from google api " + responseCode);
					return null;
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
				return jb;
		} catch (Exception e) {
			log.error("Error performing HTTP POST:" + targetURL, e);
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
}
