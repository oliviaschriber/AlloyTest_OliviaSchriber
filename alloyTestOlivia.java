import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson; //https://www.javadoc.io/doc/com.google.code.gson/gson/2.8.0/com/google/gson/Gson.html
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import java.util.Scanner;

public class alloyTestOlivia {

	public static void main(String[] args) throws IOException, InterruptedException {
		
		// auth string here
		String auth = "";
		
		// Create a reusable HttpClient
		HttpClient client = HttpClient.newHttpClient();

		// get JSON from API
		HttpRequest getRequest = HttpRequest.newBuilder().uri(URI.create("https://sandbox.alloy.co/v1/parameters"))
				.header("accept", "application/json").header("authorization", "Basic " + auth)
				.method("GET", HttpRequest.BodyPublishers.noBody()).build();
		HttpResponse<String> getResponse = client.send(getRequest,
				HttpResponse.BodyHandlers.ofString());

		Gson gsonGet = new Gson();
		
		//Create GSON Map, being sure to handle nested types as needed with a Type 
		Type type = new TypeToken<Map<String, List<Map<String, Object>>>>(){}.getType();
		Map<String, List<Map<String, Object>>> mapGetResp = gsonGet.fromJson(getResponse.body(), type);

		// Split response into required and optional inputs
		List<Map<String, Object>> reqGetResp = (List<Map<String, Object>>) mapGetResp.get("required");
		List<Map<String, Object>> optGetResp = (List<Map<String, Object>>) mapGetResp.get("optional");

		// Combine those back into one List of Maps to iterate over
		List<Map<String, Object>> allGetResp = new ArrayList<>();
		allGetResp.addAll(reqGetResp);
		allGetResp.addAll(optGetResp);

		// Create list of inputs needed
		List<String> neededInputs = new ArrayList<>();
		neededInputs.add("name_first");
		neededInputs.add("name_last");
		neededInputs.add("birth_date");
		neededInputs.add("email_address"); // note, email address from GET call doesn't have a regex, but POST fails if
											// not in right format
		neededInputs.add("address_line_1");
		neededInputs.add("address_line_2");
		neededInputs.add("address_city");
		neededInputs.add("address_state");
		neededInputs.add("address_postal_code");
		neededInputs.add("address_country_code");
		neededInputs.add("document_ssn");
		Scanner scanner = new Scanner(System.in);

		Map<String, Object> PostData = new HashMap<>();

		// hard-coded regex (minus / and /i) from POST (since didn't get from GET)
		String regexEmail = "^(([^<>()\\[\\].,;:\\s@\"]+(\\.[^<>()\\[\\].,;:\\s@\"]+)*)|(\".+\"))@(([^<>()\\[\\].,;:\\s@\"]+\\.)+[^<>()\\[\\].,;:\\s@\"]{2,})$";
		String regexBDate = "^(?:[-+]\\d{2})?(?:\\d{4}(?!\\d{2}\\b))(?:(-?)(?:(?:0[1-9]|1[0-2])(?:\\1(?:[12]\\d|0[1-9]|3[01]))?|W(?:[0-4]\\d|5[0-2])(?:-?[1-7])?|(?:00[1-9]|0[1-9]\\d|[12]\\d{2}|3(?:[0-5]\\d|6[1-6])))(?![T]$|[T][\\d]+Z$)(?:[T\\s](?:(?:(?:[01]\\d|2[0-3])(?:(:?)[0-5]\\d)?|24:?00)(?:[.,]\\d+(?!:))?)(?:\\2[0-5]\\d(?:[.,]\\d+)?)?(?:[Z]|(?:[+-])(?:[01]\\d|2[0-3])(?::?[0-5]\\d)?)?)?)?$";

		// for each needed input, look for the matching value in the GET response. Then,
		// parse out the regex, and prompt user to enter value.
//		for (String input : neededInputs) {
//			for (Map<String, Object> eachField : allGetResp) {
//				String oneField = (String) eachField.get("key");
//				if (input.equals(oneField)) {
//					
//					String regex = getRegexOverride(input);
//					
//					if (regex==null) {
//					regex = (String) eachField.get("regex");
//					}
//					getRegexOverride(regex);
//					String description = (String) eachField.get("description");
//					String answer = promptInputValidate(regex, description, scanner);
//					PostData.put(input, answer);
//				}
//			}
//		}
		//create lookup Map instead 
		Map<String, Map<String, Object>> inputLookup = new HashMap<>();
		for (Map<String, Object> field : allGetResp) {
		    inputLookup.put((String) field.get("key"), field);
		}

		//For each needed input, go through lookup Map and try and find match.
		for (String input : neededInputs) {
		    Map<String, Object> field = inputLookup.get(input);
		    if (field != null) {
		        String regex = getRegexOverride(input); //override Regex for Birth Date and Email 
		        if (regex == null) {
		            regex = (String) field.get("regex");
		        }
		        String description = (String) field.get("description");
		        String answer = promptInputValidate(regex, description, scanner);
		        PostData.put(input, answer);
		    }
		}


		scanner.close();
		handleAddress(PostData); //combine Addresses into array (needed for POST)

		// create new JSON to post
		Gson gsonPost = new Gson();
		String postDataJSON = gsonPost.toJson(PostData);

		// post to API
		HttpRequest requestPost = HttpRequest.newBuilder().uri(URI.create("https://sandbox.alloy.co/v1/evaluations"))
				.header("accept", "application/json").header("content-type", "application/json")
				.header("authorization", "Basic " + auth)
				.method("POST", HttpRequest.BodyPublishers.ofString(postDataJSON)).build();
		HttpResponse<String> responsePost = client.send(requestPost,
				HttpResponse.BodyHandlers.ofString());

		//Create GSON Map, being sure to handle nested types as needed with a Type 
		Gson gson = new Gson();
		Type responseType = new TypeToken<Map<String, Object>>() {}.getType();
		Map<String, Object> mapResponse = gson.fromJson(responsePost.body(), responseType);
	 // Map of String and Object
																							// because what is returned
																							// is not uniform data type
																							// so use object
		
		
		if (mapResponse != null) {
			Object error = mapResponse.get("error"); // if there is an error, show that
			if (error != null) {
				errorHandle(error);
			} else {
				Object mapSummary = mapResponse.get("summary"); // otherwise, find the result
				System.out.println(printOutput(mapSummary)); // and display that
			}
		}
	}
	
	private static String getRegexOverride(String input) {
	    switch (input) {
	        case "email_address":
	            return "^(([^<>()\\[\\].,;:\\s@\"]+(\\.[^<>()\\[\\].,;:\\s@\"]+)*)|(\".+\"))@(([^<>()\\[\\].,;:\\s@\"]+\\.)+[^<>()\\[\\].,;:\\s@\"]{2,})$";
	        case "birth_date":
	            return "^(?:[-+]\\d{2})?(?:\\d{4}(?!\\d{2}\\b))(?:(-?)(?:(?:0[1-9]|1[0-2])(?:\\1(?:[12]\\d|0[1-9]|3[01]))?|W(?:[0-4]\\d|5[0-2])(?:-?[1-7])?|(?:00[1-9]|0[1-9]\\d|[12]\\d{2}|3(?:[0-5]\\d|6[1-6])))(?![T]$|[T][\\d]+Z$)(?:[T\\s](?:(?:(?:[01]\\d|2[0-3])(?:(:?)[0-5]\\d)?|24:?00)(?:[.,]\\d+(?!:))?)(?:\\2[0-5]\\d(?:[.,]\\d+)?)?(?:[Z]|(?:[+-])(?:[01]\\d|2[0-3])(?::?[0-5]\\d)?)?)?)?$";
	        default:
	            return null; // No override
	    }
	}


	private static String promptInputValidate(String regex, String description, Scanner scanner) {
		System.out.println("Your " + description + "?");
		String answer = scanner.nextLine();
		while (!regex.isEmpty() && !answer.matches(regex)) {
			System.out.println("Your answer does not match regex " + regex + " Please try again.");
			System.out.println("Your " + description + "?");
			answer = scanner.nextLine();
		}
		return answer;
	}
	
	private static Map<String, Object> handleAddress(Map<String, Object> postData) {
		
		//create Map of all address lines
	    Map<String, Object> address = new HashMap<>();
	    address.put("address_line_1", postData.get("address_line_1"));
	    address.put("address_line_2", postData.get("address_line_2"));
	    address.put("address_city", postData.get("address_city"));
	    address.put("address_state", postData.get("address_state"));
	    address.put("address_postal_code", postData.get("address_postal_code"));
	    address.put("address_country_code", postData.get("address_country_code"));
	    
	    // Remove the original address fields from data 
	    postData.remove("address_line_1");
	    postData.remove("address_line_2");
	    postData.remove("address_city");
	    postData.remove("address_state");
	    postData.remove("address_postal_code");
	    postData.remove("address_country_code");

	    // Add the address map to postData
	    postData.put("addresses", List.of(address));
	    
	    return postData;
	}

	private static void errorHandle(Object error) {
		Object errorMsg = ((Map<String, Object>) error).get("message");
		Object errorDetails = ((Map<String, Object>) error).get("details");
		System.out.println("Sorry, there has been an error: " + errorMsg);
		System.out.println("Details: " + errorDetails);
	}

	private static String printOutput(Object mapSummary) {
		Object outcome = ((Map<String, Object>) mapSummary).get("outcome");
		String outcomeString = outcome.toString();
		
		switch (outcomeString) {
        case "Approved":
            return "Congratulations! You are approved.";
        case "Denied":
            return "Unfortunately, we cannot approve your application at this time.";
        case "Manual Review":
        	return "Your application is under review. Please wait for further updates.";
        default:
            return "Unable to process application."; 
    }
	}


}
