# AlloyTest_OliviaSchriber

A simple Java console application that interacts with the Alloy API to evaluate user identity data. It collects user inputs, formats the data according to Alloy's expected structure, and submits the data for evaluation. The result is displayed to the user in a clear, human-readable format.

---

## Features

- Retrieves identity parameters via `GET /v1/parameters`
- Prompts for validated user input (regex-based where applicable)
- Assembles and formats the request body
- Sends identity data via a JSON payload via an HTTP POST request
- Displays Alloy's evaluation outcome (Approved, Denied, or Manual Review)
- Graceful error handling and response feedback

---

## Requirements

- Java 11 or higher
- [Gson](https://github.com/google/gson) (already included via import)
- Alloy API credentials

---

## Setup

1. Clone this repository or copy the Java file.
2. Open the file and **replace the `auth` string** with your Alloy Basic Auth token:
   ```java
   String auth = "your_base64_encoded_auth_token_here";
   ```
3. Compile and run:
   ```bash
   javac alloyTestOlivia.java
   java alloyTestOlivia
   ```

---

## User Inputs

The following identity fields will be prompted:
  First Name, Last Name, Birth Date, Email Address, Address (Address Line 1, Address Line 2, Address City, Address State, Address Postal Code, Address Country Code) and SSN.

---

## Output

After submitting the data to Alloy, the app prints:

- **Approved** – `"Congratulations! You are approved."`
- **Denied** – `"Unfortunately, we cannot approve your application at this time."`
- **Manual Review** – `"Your application is under review. Please wait for further updates."`
- **Error** – Human-readable messages parsed from the error object

---

## Notes

- You can modify `neededInputs` to match any field returned by the `/v1/parameters` endpoint.
- The application uses `TypeToken` from Gson to avoid unchecked type warnings and ensure safe casting of JSON responses.
- Regex validation overrides are hardcoded for `email_address` and `birth_date`.

--- 

## Use of ChatGPT

ChatGPT GPT-4o mini and GPT-4o were used for:

- Improving code quality and readability
- Building incremental improvements
- Options available to parse JSON (decided on GSON) and help with formatting 
- Help with Javadoc style comments and drafting ReadMe 

---

## License

This sample is provided for educational purposes and may require further customization for production use. Always follow [Alloy's API documentation](https://docs.alloy.com/) for full integration details.

---

