document.addEventListener("DOMContentLoaded", () => {
  const registerButton = document.getElementById("register-btn");
  const loginButton = document.getElementById("login-btn");

  // Helper function to display error message
  const showError = (elementId, message) => {
    const errorElement = document.getElementById(elementId);
    errorElement.innerText = message;
    errorElement.style.display = 'block';
  };

  // Helper function to hide error message
  const hideError = (elementId) => {
    const errorElement = document.getElementById(elementId);
    errorElement.style.display = 'none';
  };

  registerButton.addEventListener("click", async () => {
    const userId = document.getElementById("userid").value;
    const username = document.getElementById("username").value;
    const displayName = document.getElementById("displayname").value;
    const phone = document.getElementById("phone").value;

    // Validate input fields
    let isValid = true;
    if (!userId) {
      showError("userid-error", "User ID is required.");
      isValid = false;
    } else {
      hideError("userid-error");
    }

    if (!username) {
      showError("username-error", "Username is required.");
      isValid = false;
    } else {
      hideError("username-error");
    }

    if (!displayName) {
      showError("displayname-error", "Display Name is required.");
      isValid = false;
    } else {
      hideError("displayname-error");
    }

    if (!phone) {
      showError("phone-error", "Phone number is required.");
      isValid = false;
    } else {
      hideError("phone-error");
    }

    if (!isValid) return;

    const data = { id: userId, name: username, displayName, phoneNumber: phone };

    try {
      const response = await fetch('http://localhost:8080/api/register/options', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });

      if (!response.ok) throw new Error("Failed to get registration options");

      const publicKeyCredentialCreationOptionsJSON = await response.json();
      console.log(JSON.stringify(publicKeyCredentialCreationOptionsJSON));

      // modify response as the WebAuthn spec
      delete publicKeyCredentialCreationOptionsJSON.hints;
      publicKeyCredentialCreationOptionsJSON.challenge = publicKeyCredentialCreationOptionsJSON.challenge.value;

      const credentialCreationOptions = PublicKeyCredential.parseCreationOptionsFromJSON(publicKeyCredentialCreationOptionsJSON);
      const publicKeyCredential = await navigator.credentials.create({ publicKey: credentialCreationOptions });

      const registrationResponseJSON = publicKeyCredential.toJSON();
      console.log(JSON.stringify(registrationResponseJSON));

      const requestBody = {
        userId,
        'registrationResponseJSON': JSON.stringify(registrationResponseJSON)
      };

      const validateResponse = await fetch('http://localhost:8080/api/register/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
      });

      if (validateResponse.ok) {
        alert("Registration successful!");
      } else {
        const errorMessage = await validateResponse.text();
        showError("register-error", `Error: ${errorMessage}`);
      }

    } catch (error) {
      console.error('Registration failed:', error);
      showError("register-error", "Registration failed. Please try again.");
    }
  });

  loginButton.addEventListener("click", async () => {
    const loginId = document.getElementById("loginid").value;

    // Validate login input
    if (!loginId) {
      showError("loginid-error", "Login ID is required.");
      return;
    } else {
      hideError("loginid-error");
    }

    const data = { id: loginId };

    try {
      const response = await fetch('http://localhost:8080/api/authenticate/options', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });

      if (!response.ok) throw new Error("Failed to get login options");

      const publicKeyCredentialRequestOptionsJSON = await response.json();
      console.log(JSON.stringify(publicKeyCredentialRequestOptionsJSON));

      const credentialGetOptions = PublicKeyCredential.parseRequestOptionsFromJSON(publicKeyCredentialRequestOptionsJSON);
      const publicKeyCredential = await navigator.credentials.get({ publicKey: credentialGetOptions });

      const authenticationResponseJSON = publicKeyCredential.toJSON();
      console.log("authenticationResponseJSON: %s", JSON.stringify(authenticationResponseJSON));

      const requestBody = {
        userId: loginId,
        'authenticateResponseJSON': JSON.stringify(authenticationResponseJSON)
      };

      const validateResponse = await fetch('http://localhost:8080/api/authenticate/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
      });

      if (validateResponse.ok) {
        alert("Login successful!");
        window.location.href = '/dashboard';  // Redirect to dashboard
      } else {
        const errorMessage = await validateResponse.text();
        showError("login-error", `Error: ${errorMessage}`);
      }

    } catch (error) {
      console.error('Login failed:', error);
      showError("login-error", "Login failed. Please try again.");
    }
  });
});
