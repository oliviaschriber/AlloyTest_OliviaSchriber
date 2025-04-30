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
		String auth = System.getenv("Alloy_Auth");

		// Create a reusable HttpClient
		HttpClient client = HttpClient.newHttpClient();

		// get JSON from API
		HttpRequest getRequest = HttpRequest.newBuilder().uri(URI.create("https://sandbox.alloy.co/v1/parameters"))
				.header("accept", "application/json").header("authorization", "Basic " + auth)
				.method("GET", HttpRequest.BodyPublishers.noBody()).build();
		HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());

		Gson gsonGet = new Gson();

		// Create GSON Map to be able to handle nested types as needed with a Type
		Type type = new TypeToken<Map<String, List<Map<String, Object>>>>() {
		}.getType();
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
		neededInputs.add("email_address");
		neededInputs.add("address_line_1");
		neededInputs.add("address_line_2");
		neededInputs.add("address_city");
		neededInputs.add("address_state");
		neededInputs.add("address_postal_code");
		neededInputs.add("address_country_code");
		neededInputs.add("document_ssn");

		// Create scanner for system input, and map for data to post
		Scanner scanner = new Scanner(System.in);
		Map<String, Object> postData = new HashMap<>();

		// create lookup Map for the getResp Map to aid in lookup next
		Map<String, Map<String, Object>> inputLookup = new HashMap<>();
		for (Map<String, Object> field : allGetResp) {
			inputLookup.put((String) field.get("key"), field);
		}

		// For each needed input, go through lookup Map and try and find match on key
		for (String input : neededInputs) {
			Map<String, Object> field = inputLookup.get(input);
			if (field != null) {
				String regex = getRegexOverride(input); // override Regex for Birth Date and Email
				if (regex == null) {
					regex = (String) field.get("regex");
				}
				String description = (String) field.get("description"); // get "name" of item we're prompting for
				String answer = promptInputValidate(regex, description, scanner); // prompt for data
				postData.put(input, answer); // add that data to map to post, with input as key and answer as value
			}
		}

		scanner.close();
		handleAddress(postData); // combine Addresses into array (needed for POST)

		// create new JSON to post
		Gson gsonPost = new Gson();
		String postDataJSON = gsonPost.toJson(postData);

		// post to API
		HttpRequest requestPost = HttpRequest.newBuilder().uri(URI.create("https://sandbox.alloy.co/v1/evaluations"))
				.header("accept", "application/json").header("content-type", "application/json")
				.header("authorization", "Basic " + auth)
				.method("POST", HttpRequest.BodyPublishers.ofString(postDataJSON)).build();
		HttpResponse<String> responsePost = client.send(requestPost, HttpResponse.BodyHandlers.ofString());

		// Create GSON Map, being sure to handle nested types as needed with a Type
		Gson gson = new Gson();
		Type responseType = new TypeToken<Map<String, Object>>() {
		}.getType();
		Map<String, Object> mapResponse = gson.fromJson(responsePost.body(), responseType);

		// error handling: make sure we have a response
		if (mapResponse != null) {
			Object error = mapResponse.get("error");
			if (error != null) {
				errorHandle(error); // if there is an error, show that
			} else {
				Object mapSummary = mapResponse.get("summary"); // otherwise, find the result
				System.out.println(printOutput(mapSummary)); // and display that
			}
		}
	}

	/**
	 * Returns a regex pattern override for specific input fields, if needed.
	 * Currently overrides the default patterns for "email_address" and
	 * "birth_date".
	 *
	 * @param input The input key (e.g., "email_address", "birth_date").
	 * @return A custom regex string for validation, or null if no override is
	 *         required.
	 */
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

	/**
	 * Prompts the user for input and validates it against a provided regex pattern.
	 * If the input is invalid, the user is repeatedly prompted until valid input is
	 * provided.
	 *
	 * @param regex       The regex pattern to validate the user input.
	 * @param description A descriptive label of the input field (e.g., "Date of
	 *                    Birth").
	 * @param scanner     A Scanner object for reading input from the console.
	 * @return The validated input as a string.
	 */
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

	/**
	 * Consolidates individual address components into a single address map and
	 * inserts it into the post data under the "addresses" key. Removes the original
	 * individual address fields.
	 *
	 * @param postData The data map that will be sent in the POST request.
	 * @return The updated data map with the consolidated address.
	 */
	private static Map<String, Object> handleAddress(Map<String, Object> postData) {

		// create Map of all address lines
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

	/**
	 * Handles API errors by extracting and printing the error message and details
	 * in a user-friendly format.
	 *
	 * @param error The error object returned by the API response.
	 */
	private static void errorHandle(Object error) {

		// Using TypeToken for safe casting to a Map to be able to get the message and
		// details
		Gson gson = new Gson();
		Type errorType = new TypeToken<Map<String, Object>>() {
		}.getType();
		Map<String, Object> errorMap = gson.fromJson(gson.toJson(error), errorType);

		// get error message + details and display
		Object errorMsg = errorMap.get("message");
		Object errorDetails = errorMap.get("details");
		System.out.println("Sorry, there has been an error: " + errorMsg);
		System.out.println("Details: " + errorDetails);
	}

	/**
	 * Parses the outcome from the API summary and returns an appropriate message
	 * for the user.
	 *
	 * @param mapSummary The summary object from the API response containing the
	 *                   "outcome" field.
	 * @return A message string based on the outcome: Approved, Denied, or Manual
	 *         Review.
	 */
	private static String printOutput(Object mapSummary) {

		// Using TypeToken for safe casting to a Map to be able to get the outcome from
		// the summary
		Gson gson = new Gson();
		Type summaryType = new TypeToken<Map<String, Object>>() {
		}.getType();
		Map<String, Object> summaryMap = gson.fromJson(gson.toJson(mapSummary), summaryType);

		// getting the outcome directly
		String outcome = summaryMap.get("outcome").toString();

		switch (outcome) {
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
