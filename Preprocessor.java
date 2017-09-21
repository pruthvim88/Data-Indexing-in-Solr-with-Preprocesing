package com.InfoRetrProject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;


public class Preprocessor {

	public static void main(String[] args) throws Exception {
		FileReader f = new FileReader("E:\\E drive\\Study\\UB\\CS 535 Information Retrieval\\IRProject1\\Final Tweets\\Korean_10k_Tweets.json");
		BufferedReader br = new BufferedReader(f);

		JSONArray jsonObjectArray = new JSONArray();
		    String line;
		    while ((line = br.readLine()) != null) {
		    	System.out.println(jsonObjectArray.length());
		    	try {
		    		JSONObject j = new JSONObject(line);
			    	jsonObjectArray.put(j);
		    	}
		    	catch(Exception e) {
		    		System.out.println("Skipped a tweet");
		    	}
		    	
		    }
		    br.close();
		    f.close();

	FileWriter fw = new FileWriter("E:\\E drive\\Study\\UB\\CS 535 Information Retrieval\\IRProject1\\Final Tweets\\Project Tweets\\Solr\\Korean_V2.json");
	BufferedWriter bw = new BufferedWriter(fw);
	System.out.println(jsonObjectArray.length());
	HashSet<String> emoticons = GetAllEmoticons();
	int count = 0;
	System.out.println("Tweet JSON Array Length = " + jsonObjectArray.length());
	for(int i = 0; i<12; i++) 
	{
		System.out.println("Count = " + count);
		for(Object tweet : jsonObjectArray)
		{
				ArrayList<String> hashtagsFilteredList = null;
				JSONObject tweetJson = (JSONObject) tweet;
				String hashTagField = null;
			try {	
			
			JSONArray hashtagsJson = (JSONArray) tweetJson.get("hashtags");
			hashtagsFilteredList = GetHashTags(hashtagsJson);
			hashTagField = MakeCSV(hashtagsFilteredList);
			}catch(Exception e)
			{
				System.out.println("Skipped hashtags");
			}
			
			String mentionsField = null;
			ArrayList<String> mentionsString = null;
			try {
			mentionsString = getMentionsFromJSON(tweetJson.get("mentions"));
			mentionsField = MakeCSV(mentionsString);
			}catch(Exception e) { System.out.println("Exception for mentions field"); }
			
			String dateString = getDateInFormat(tweetJson.get("tweet_date"));
			ArrayList<String> tweetUrls = null;
			String urlField = null;
			try {
			tweetUrls = getTweetURLs(tweetJson.get("tweet_urls"));
			urlField = MakeCSV(tweetUrls);
			} catch(Exception e) {
				System.out.println();
			}
			String tweet_text = null;
			try {tweet_text =  tweetJson.getString("tweet_text");
			tweet_text = GetRandomString(tweet_text);
			System.out.println(tweet_text);
			} catch(Exception e) {System.out.println(); }
			ArrayList<String> tweetEmos = getEmoticons(emoticons, tweet_text);
			String emojiField = MakeCSV(tweetEmos);
			
			String geolocation = null;
			try {
				String tweet_loc = tweetJson.get("tweet_loc").toString();
				geolocation = getLocation(tweet_loc);
			} catch(Exception e1) {
				//System.out.println("Skipped location tweet");
			}
			ArrayList<String> stopChars = new ArrayList<String>();
			if(tweetEmos != null) 
			stopChars.addAll(tweetEmos);
			if(tweetUrls != null)
			stopChars.addAll(tweetUrls);
			if(mentionsString != null)
			stopChars.addAll(mentionsString);
			if(hashtagsFilteredList!= null)
			stopChars.addAll(hashtagsFilteredList);
			String lang = null;
			try {
			lang =  tweetJson.getString("tweet_lang");
			} catch(Exception e1) { System.out.println("Skipped for language tweet"); }
			String tweet_text_lang = null;
			tweet_text_lang = RemoveStopChars(tweet_text, stopChars);
			if(lang!= null && !lang.contains("ko")) {
			tweet_text_lang = tweet_text_lang.replaceAll("\\W", " ");
			 }
			else {
				tweet_text_lang = tweet_text_lang.replaceAll("#", "");
				tweet_text_lang = tweet_text_lang.replaceAll(",", "");
				System.out.println(tweet_text_lang);
			}
			JSONObject finalJSon = new JSONObject();
			String topic  = GetTopicFromTweet(tweet_text);
			System.out.println(topic);
			finalJSon.put("topic", "Entertainment");
			finalJSon.put("tweet_text",tweet_text);
			
			finalJSon.put("tweet_lang", lang);
			if(lang != null && lang.contains("es"))
				finalJSon.put("text_es", tweet_text_lang );
			if(lang != null && lang.contains("ko"))
				finalJSon.put("text_ko", tweet_text_lang );
			if(lang != null && lang.contains("tr"))
				finalJSon.put("text_tr", tweet_text_lang );
			if(lang != null && lang.contains("en"))
				finalJSon.put("text_en", tweet_text_lang );
			finalJSon.put("hashtags", hashTagField);
			finalJSon.put("mentions", mentionsField);
			finalJSon.put("tweet_urls", urlField);
			finalJSon.put("tweet_emoticons", emojiField);
			finalJSon.put("tweet_date", dateString );
			finalJSon.put("tweet_loc", geolocation);
			PrintWriter pw = new PrintWriter(bw);
			pw.println(finalJSon);
			System.out.println("Number of tweets writter in file = " + count++);
		}
	}
		
	}
	
	
	private static String GetRandomString(String tweet_text) {
	
		List<String> splited = new ArrayList<String>((tweet_text.length() + 3 - 1) / 3);

	    for (int start = 0; start < tweet_text.length(); start += 3) {
	    	splited.add(tweet_text.substring(start, Math.min(tweet_text.length(), start + 3)));
	    }
		StringBuilder randTweet = new StringBuilder();
		if(tweet_text == null)
			return null;
		int count = 0;
		while(randTweet.length() <139 && count < 20)
		{
			count++;
			int wordIndex = ThreadLocalRandom.current().nextInt(0, splited.size()-1);
			if(randTweet.length() + splited.get(wordIndex).length() < 140)
				randTweet.append(splited.get(wordIndex));
		}
		
		return randTweet.toString();
	}


	private static String GetTopicFromTweet(String tweet_text) {
		 String[] apple = {"Iphone", "Apple", "Iphone7", "Ipone6"};
		 String[] uspe = {"US Presidential Elections"};
		 String[] usopen = {"USOpen","US Open", "USOpen"};
		 String[] syria = {"Syrian Civil War","Syria Civil War", "Syria"};
		 String[] got = {"Game of Thrones", "GOT", "Jon Snow", "Tyrion Lannister"};
		 if(tweet_text == null)
			 	System.out.println("Null Tweet Returning");
		 for(String app : apple)
		 {
			 if(tweet_text.contains(app))
				return "Tech";
		 }
		 for(String app : uspe)
		 {
			 if(tweet_text.contains(app))
				return "Politics";
		 }
		 for(String app : usopen)
		 {
			 if(tweet_text.contains(app))
			 {
				 System.out.println(tweet_text);
				 return "Sports";	 
			 }	
		 }
		 for(String app : syria)
		 {
			 if(tweet_text.contains(app))
				return "World News";
		 }
		 for(String app : got)
		 {
			 if(tweet_text.contains(app))
				return "Entertainment";
		 }
		 return "Sports";
	}


	private static String RemoveStopChars(String tweet_text, ArrayList<String> stopChars) {
		
		if(tweet_text == null) return null;
		StringBuilder s = new StringBuilder(tweet_text);
		for(String cur : stopChars)
		{
			int index = s.indexOf(cur);
			if(index != -1)
			{
				int length = cur.length();
				s.delete(index, index + length);
			}
		}
		String punctuationRemovedString = s.toString();
		return punctuationRemovedString.trim().replaceAll(" +", " ");
	}


	private static ArrayList<String> getEmoticons(HashSet<String> emoticons, String tweetText) throws Exception {
		
		ArrayList<String> tweetEmos = new ArrayList<String>();
		if(tweetText == null) return null;
		for(String emo : emoticons)
		{
			if(tweetText.contains(emo))
			{
				tweetEmos.add(emo);
			}
		}
	    String regexPattern = "[\uD83C-\uDBFF\uDC00-\uDFFF]+";
        byte[] utf8 = tweetText.getBytes("UTF-16");

        String string1 = new String(utf8, "UTF-16");

        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(string1);
        ArrayList<String> matchList = new ArrayList<String>();

        while (matcher.find()) {
            matchList.add(matcher.group());
        }
		tweetEmos.addAll(getAllEmojis(tweetText));
		tweetEmos.addAll(matchList);
		return tweetEmos;
	}
	 public static ArrayList<String> getAllEmojis(String str) {
         ArrayList<String> emojis = new ArrayList<String>();
         for (Emoji emoji : EmojiManager.getAll()) {
             if (str.contains(emoji.getUnicode()))
             {
            	 emojis.add(emoji.getUnicode());
             }
         }
         return emojis;
     }


	private static HashSet<String> GetAllEmoticons() {
		
		HashSet<String> emoticons = GetAllEmoticonsFromFile("/Users/vaibhav/Google Drive/Acads/IR/Project/emoticons.txt");
		HashSet<String> kaomojis = GetAllEmoticonsFromFile("/Users/vaibhav/Google Drive/Acads/IR/Project/kaomoji.txt");
		emoticons.addAll(kaomojis);
		return emoticons;
	}
	
	public static HashSet<String> GetAllEmoticonsFromFile(String file)
	{
		HashSet<String> emoticons = new HashSet<String>();
		try
		{
		InputStream inputstream = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(inputstream, Charset.forName("UTF-16")));

		    String line;
		    while ((line = br.readLine()) != null) {
		    	emoticons.add(line);
		    }
		    br.close();
		    inputstream.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return emoticons;
	}


	private static ArrayList<String> getTweetURLs(Object object) {
		
		ArrayList<String> tweetURLsField = new ArrayList<String>();
		JSONArray tweetURLsArray = (JSONArray) object;
		for(Object o : tweetURLsArray)
		{
			JSONObject curUrl = (JSONObject) o;
			String displayURL = curUrl.getString("displayURL");
			String text = curUrl.getString("text");
			String expandedURL = curUrl.getString("expandedURL");
			String URL = curUrl.getString("URL");
			if(displayURL != null)
				tweetURLsField.add(displayURL);
			if(text != null)
				tweetURLsField.add(text);
			if(expandedURL != null)
				tweetURLsField.add(expandedURL);
			if(URL != null)
				tweetURLsField.add(URL);
		}
		return tweetURLsField;
	}


	private static String getLocation(String tweet_loc) {
		
		if(tweet_loc == null)
			return null;
		int indexLat = tweet_loc.indexOf("latitude");
		int indexLon = tweet_loc.indexOf("longitude");
		int indexComma = tweet_loc.indexOf(",");
		String lattitude = tweet_loc.substring(indexLat + 9, indexComma);
		String longitude = tweet_loc.substring(indexLon + 10, tweet_loc.length()-1);
		
		return lattitude + "," + longitude;
	}


	private static String MakeCSV(ArrayList<String> fieldArray) {
		StringBuilder fieldCsv = new StringBuilder();
		boolean appended = false;
		if(fieldArray == null) return null;
		for(String text : fieldArray )
		{
			appended = true;
			fieldCsv.append(text);
			fieldCsv.append(",");
		}
		if(fieldCsv.length() > 0)
			fieldCsv = fieldCsv.deleteCharAt( fieldCsv.length()-1);
		if(appended)
			return fieldCsv.toString();
		else
			return null;
	}

	private static String getDateInFormat(Object object) throws ParseException {
		String dateStr = (String) object;
		String dateFormat  = "dd MMM yyyy HH:mm:ss z";
		Date date = new SimpleDateFormat(dateFormat).parse(dateStr);		
		String solrDateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";
		String solrDate = new SimpleDateFormat(solrDateFormat).format(date);
		
		return solrDate.toString();
	}

	private static ArrayList<String> getMentionsFromJSON(Object object) {
		
		ArrayList<String> mentionField = new ArrayList<String>();
		JSONArray mentionsArray = (JSONArray) object;
		for(Object o : mentionsArray)
		{
			JSONObject curMention = (JSONObject) o;
			String name = curMention.getString("name");
			String text = curMention.getString("text");
			String screenname = curMention.getString("screenName");
			if(name != null && text!= null && screenname != null)
			{
				mentionField.add(name);
				mentionField.add(text);
				mentionField.add(screenname);
			}
		}
		return mentionField;
	}

	public static ArrayList<String> GetHashTags(JSONArray hashtagsJson)
	{
		ArrayList<String> filteredHashtags = new ArrayList<String>();
		
		for(Object o : hashtagsJson)
		{
			JSONObject hashtagJson = (JSONObject) o;
			String hashtag = hashtagJson.getString("text");
			if(hashtag != null)
			filteredHashtags.add(hashtag);
		}
		
		
		
		return filteredHashtags;
	}
}

