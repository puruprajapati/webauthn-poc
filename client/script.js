document.addEventListener("DOMContentLoaded", () => {
  const registerButton = document.getElementById("register-btn");
  const loginButton = document.getElementById("login-btn");

  registerButton.addEventListener("click", async () => {
    const userId = document.getElementById("userid").value;
    const username = document.getElementById("username").value;
    const displayName = document.getElementById("displayname").value;
    const phone = document.getElementById("phone").value;

    const data = { id: userId, name: username, displayName, phoneNumber: phone };

    try {
      const response = await fetch('http://localhost:8080/api/register/options', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });

      const publicKeyCredentialCreationOptionsJSON = await response.json() // convert to JSONObject

      console.log(JSON.stringify(publicKeyCredentialCreationOptionsJSON));

      // modify response as the webAuthnSpec
      delete publicKeyCredentialCreationOptionsJSON.hints;
      publicKeyCredentialCreationOptionsJSON.challenge = publicKeyCredentialCreationOptionsJSON.challenge.value;

      const credentialCreationOptions = PublicKeyCredential.parseCreationOptionsFromJSON(publicKeyCredentialCreationOptionsJSON); // convert to PublicKeyCredentialCreationOptions
      const publicKeyCredential = await navigator.credentials.create({ publicKey: credentialCreationOptions }); // create PublicKeyCredential

      const registrationResponseJSON = publicKeyCredential.toJSON(); // convert to JSONObject
      console.log(JSON.stringify(registrationResponseJSON));

      const requestBody = {
        userId,
        'registrationResponseJSON': JSON.stringify(registrationResponseJSON) //convert to string
      }

      const validateResponse = await fetch('http://localhost:8080/api/register/verify', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBody)
      });

      console.log(JSON.stringify(validateResponse));

    } catch (error) {
      console.error('Registration failed:', error);
    }
  });

  loginButton.addEventListener("click", async () => {
    const loginId = document.getElementById("loginid").value;

    const data = { id: loginId };

    try {
      const response = await fetch('http://localhost:8080/api/authenticate/options', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });

      const publicKeyCredentialRequestOptionsJSON = await response.json() // convert to JSONObject

      console.log(JSON.stringify(publicKeyCredentialRequestOptionsJSON));

      const credentialGetOptions = PublicKeyCredential.parseRequestOptionsFromJSON(publicKeyCredentialRequestOptionsJSON);
      const publicKeyCredential = await navigator.credentials.get({ publicKey: credentialGetOptions });

      const authenticationResponseJSON = publicKeyCredential.toJSON();
      console.log("authenticationResponseJSON: %s", authenticationResponseJSON);

      const requestBody = {
        userId: loginId,
        'authenticateResponseJSON': JSON.stringify(authenticationResponseJSON) //convert to string
      }

      const validateResponse = await fetch('http://localhost:8080/api/authenticate/verify', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBody)
      });

      console.log("Response: %s", validateResponse.json());
      console.log("Authenticated")


    } catch (error) {
      console.error('Login failed:', error);
    }
  });
});
