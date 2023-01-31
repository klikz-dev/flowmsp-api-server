package com.flowmsp.service.MessageParser;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserFactory {
    private static final Logger log = LoggerFactory.getLogger(ParserFactory.class);

    public static MsgParser CreateObject(String source, String parser) {
        //As we will add new parser we will change the handler here

        log.debug("parser requested", parser);

        parser = Strings.nullToEmpty(parser);
        if (source.equalsIgnoreCase("email")) {
            if (parser.equalsIgnoreCase("Standard")) {
                return new StandardEmail();
            } else if (parser.equalsIgnoreCase("hrfd")) {
                return new HrfdEmail();
            } else if (parser.equalsIgnoreCase("North Park")) {
                return new NorthParkEmail();
            } else if (parser.equalsIgnoreCase("Amboy")) {
                return new AmboyEmail();
            } else if (parser.equalsIgnoreCase("Janesville")) {
                return new JanesvilleEmail();
            } else if (parser.equalsIgnoreCase("Kankakee")) {
                return new KankakeeEmail();
            } else if (parser.equalsIgnoreCase("Romeoville")) {
                return new RomeovilleEmail();
            } else if (parser.equalsIgnoreCase("Litchfield")) {
                return new LitchfieldEmail();
            } else if (parser.equalsIgnoreCase("Streator")) {
                return new StreatorEmail();
            } else if (parser.equalsIgnoreCase("Oak Lawn")) {
                return new OakLawnEmail();
            } else if (parser.equalsIgnoreCase("Hiplink")) {
                return new HiplinkEmail();
            } else if (parser.equalsIgnoreCase("Carlinville")) {
                return new CarlinvilleEmail();
            } else if (parser.equalsIgnoreCase("Hebron")) {
                return new HebronEmail();
            } else if (parser.equalsIgnoreCase("Westmont")) {
                return new WestmontEmail();
            } else if (parser.equalsIgnoreCase("TriState")) {
                return new TriStateEmail();
            } else if (parser.equalsIgnoreCase("Savoy")) {
                return new SavoyEmail();
            } else if (parser.equalsIgnoreCase("Urbana")) {
                return new UrbanaEmail();
            } else if (parser.equalsIgnoreCase("Crestwood")) {
                return new CrestwoodEmail();
            } else if (parser.equalsIgnoreCase("Boles")) {
                return new BolesEmail();
            } else if (parser.equalsIgnoreCase("Ogle")) {
                return new OgleEmail();
            } else if (parser.equalsIgnoreCase("Belleville")) {
                return new BellevilleEmail();
            } else if (parser.equalsIgnoreCase("RockCom")) {
                return new RockComEmail();
            } else if (parser.equalsIgnoreCase("Woodcomm")) {
                return new WoodcommEmail();
            } else if (parser.equalsIgnoreCase("DuPageCounty")) {
                return new DuPageCountyEmail();
            } else if (parser.equalsIgnoreCase("Homewood")) {
                return new HomewoodEmail();
            } else if (parser.equalsIgnoreCase("SouthElgin")) {
                return new SouthElginEmail();
            } else if (parser.equalsIgnoreCase("Mehlville")) {
                return new MehlvilleEmail();
            } else if (parser.equalsIgnoreCase("Jacksonville")) {
                return new JacksonvilleEmail();
            } else if (parser.equalsIgnoreCase("Gilman")) {
                return new GilmanEmail();
            } else if (parser.equalsIgnoreCase("GlenCarbon")) {
                return new GlenCarbonEmail();
            } else if (parser.equalsIgnoreCase("Sikeston")) {
                return new SikestonEmail();
            } else if (parser.equalsIgnoreCase("Mattoon")) {
                return new MattoonEmail();
            } else if (parser.equalsIgnoreCase("SpringLake")) {
                return new SpringLakeEmail();
            } else if (parser.equalsIgnoreCase("TriTownship")) {
                return new TriTownshipEmail();
            } else if (parser.equalsIgnoreCase("Goodfield")) {
                return new GoodfieldEmail();
            } else if (parser.equalsIgnoreCase("pekinfiredep")) {
                return new pekinfiredepEmail();
            } else if (parser.equalsIgnoreCase("barringtonco")) {
                return new barringtoncoEmail();
            } else if (parser.equalsIgnoreCase("sandbox")) {
                return new sandboxEmail();
            } else if (parser.equalsIgnoreCase("crestwoodfir2")) {
                return new crestwoodfir2();
            } else if (parser.equalsIgnoreCase("highridgefir")) {
                return new highridgefir();
            } else if (parser.equalsIgnoreCase("eurekafirepr")) {
                return new eurekafirepr();
            } else if (parser.equalsIgnoreCase("saddleriverf")) {
                return new saddleriverf();
            } else if (parser.equalsIgnoreCase("victoriafire")) {
                return new victoriafire();
            } else if (parser.equalsIgnoreCase("ishpemingfir")) {
                return new ishpemingfir();
            } else if (parser.equalsIgnoreCase("norwalkfire")) {
                return new norwalkfire();
            } else if (parser.equalsIgnoreCase("tazewellfire")) {
                return new tazewellfire();
            } else if (parser.equalsIgnoreCase("emporiafired")) {
                return new emporiafired();
            } else if (parser.equalsIgnoreCase("mcleanfirede")) {
                return new mcleanfirede();
            } else if (parser.equalsIgnoreCase("lakesfoursea")) {
                return new lakesfoursea();
            } else if (parser.equalsIgnoreCase("piercecounty")) {
                return new piercecounty();
            } else if (parser.equalsIgnoreCase("ottawafirede")) {
                return new ottawafirede();
            } else if (parser.equalsIgnoreCase("bathtwpfire")) {
                return new bathtwpfire();
            } else if (parser.equalsIgnoreCase("hinckleyfire")) {
                return new hinckleyfire();
            } else if (parser.equalsIgnoreCase("paxtonfirede")) {
                return new paxtonfirede();
            } else if (parser.equalsIgnoreCase("westplainsfi")) {
                return new westplainsfi();
            } else if (parser.equalsIgnoreCase("covecreekvol")) {
                return new covecreekvol();
            } else if (parser.equalsIgnoreCase("dartmouthfir")) {
                return new dartmouthfir();
            } else if (parser.equalsIgnoreCase("wythevillefi")) {
                return new wythevillefi();
            } else if (parser.equalsIgnoreCase("dartmouthfir2")) {
                return new dartmouthfir2();
            } else if (parser.equalsIgnoreCase("dublinfirede")) {
                return new dublinfirede();
            } else if (parser.equalsIgnoreCase("osagebeachfi")) {
                return new osagebeachfi();
            } else if (parser.equalsIgnoreCase("valcom")) {
                return new valcom();
            } else if (parser.equalsIgnoreCase("russellfired")) {
                return new russellfired();
            } else if (parser.equalsIgnoreCase("morgancounty")) {
                return new morgancounty();
            } else if (parser.equalsIgnoreCase("marlowvolfir")) {
                return new marlowvolfir();
            } else if (parser.equalsIgnoreCase("highlandvill")) {
                return new highlandvill();
            } else if (parser.equalsIgnoreCase("porterfirede")) {
                return new porterfirede();
            } else if (parser.equalsIgnoreCase("whitesviller")) {
                return new whitesviller();
            } else if (parser.equalsIgnoreCase("southcentral")) {
                return new southcentral();
            } else if (parser.equalsIgnoreCase("bigriverfire")) {
                return new bigriverfire();
            } else if (parser.equalsIgnoreCase("wake_county_nc")) {
                return new wake_county_nc();
            } else if (parser.equalsIgnoreCase("santafefirep")) {
                return new santafefirep();
            } else if (parser.equalsIgnoreCase("zillahfire")) {
                return new zillahfire();
            } else if (parser.equalsIgnoreCase("wintergarden2")) {
                return new wintergarden2();
            } else if (parser.equalsIgnoreCase("whiteplainsv")) {
                return new whiteplainsv();
            } else if (parser.equalsIgnoreCase("habershamga")) {
                return new habershamga();
            } else if (parser.equalsIgnoreCase("psap2437")) {
                return new psap2437();
            } else if (parser.equalsIgnoreCase("cencom2152")) {
                return new cencom2152();
            } else if (parser.equalsIgnoreCase("kenai_borough")) {
                return new kenai_borough();
            } else if (parser.equalsIgnoreCase("sandbox_psap")) {
                return new sandbox_psap();
            } else if (parser.equalsIgnoreCase("loneoakfired")) {
                return new loneoakfired();
            } else if (parser.equalsIgnoreCase("psap2262")) {
                return new psap2262();
            } else if (parser.equalsIgnoreCase("rockcommunit")) {
                return new rockcommunit();
            } else if (parser.equalsIgnoreCase("mazonfirepro")) {
                return new mazonfirepro();
            } else if (parser.equalsIgnoreCase("northjackson")) {
                return new northjackson();
            } else if (parser.equalsIgnoreCase("montgomery")) {
                return new montgomery();
            } else if (parser.equalsIgnoreCase("psap1667")) {
                return new psap1667();
            } else if (parser.equalsIgnoreCase("psap3935")) {
                return new psap3935();
            } else if (parser.equalsIgnoreCase("somersetfire")) {
                return new somersetfire();
            } else if (parser.equalsIgnoreCase("delhitwpfire")) {
                return new delhitwpfire();
            } else if (parser.equalsIgnoreCase("pittsvillefi")) {
                return new pittsvillefi();
            } else if (parser.equalsIgnoreCase("rantoulfire")) {
                return new rantoulfire();
            } else if (parser.equalsIgnoreCase("clinton1926")) {
                return new clinton1926();
            } else if (parser.equalsIgnoreCase("coconino428")) {
                return new coconino428();
            } else if (parser.equalsIgnoreCase("franklintn911")) {
                return new franklintn911();
            } else if (parser.equalsIgnoreCase("rutherford4428")) {
                return new rutherford4428();
            } else if (parser.equalsIgnoreCase("ecom8131")) {
                return new ecom8131();
            } else if (parser.equalsIgnoreCase("kirkwood3838")) {
                return new kirkwood3838();
            } else
                log.info("defaulting to StandardEmail parser");
            return new StandardEmail();
        } else {
            if (parser.equalsIgnoreCase("Standard")) {
                return new StandardSMS();
            } else {
                return new StandardSMS();
            }
        }
    }
}
