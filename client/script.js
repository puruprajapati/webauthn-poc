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
    const username = userId;
    const displayName = "";
    const phone = "";
    // const username = document.getElementById("username").value;
    // const displayName = document.getElementById("displayname").value;
    // const phone = document.getElementById("phone").value;

    // Validate input fields
    let isValid = true;
    if (!userId) {
      showError("userid-error", "User ID is required.");
      isValid = false;
    } else {
      hideError("userid-error");
    }

    // if (!username) {
    //   showError("username-error", "Username is required.");
    //   isValid = false;
    // } else {
    //   hideError("username-error");
    // }

    // if (!displayName) {
    //   showError("displayname-error", "Display Name is required.");
    //   isValid = false;
    // } else {
    //   hideError("displayname-error");
    // }

    // if (!phone) {
    //   showError("phone-error", "Phone number is required.");
    //   isValid = false;
    // } else {
    //   hideError("phone-error");
    // }

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

      // // modify response as the WebAuthn spec
      // delete publicKeyCredentialCreationOptionsJSON.hints;
      // publicKeyCredentialCreationOptionsJSON.challenge = publicKeyCredentialCreationOptionsJSON.challenge.value;

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
    try {
      // For resident keys, we don't specify a userID - let the authenticator show available credentials
      const data = {};

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

      // Extract userID from the credential response (resident key stores it)
      // The userID is typically in the response, we can get it from the parsed credential
      let userId = null;

      // Try to get userID from the response - this depends on how your server returns it
      // For resident keys, the authenticator response should contain the userID
      if (publicKeyCredential.response && publicKeyCredential.response.userHandle) {
        // userHandle is the userID encoded in the credential response
        const userHandleArray = new Uint8Array(publicKeyCredential.response.userHandle);
        userId = new TextDecoder().decode(userHandleArray);
      }

      if (!userId) {
        throw new Error("User ID not found in credential response. Make sure you're using resident keys.");
      }

      const requestBody = {
        userId: userId,
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

  async function checkBluetoothEnabled() {
    if (!navigator.bluetooth) {
      throw new Error("Bluetooth not supported on this device");
    }

    try {
      const device = await navigator.bluetooth.requestDevice({ acceptAllDevices: true });
      console.log("Bluetooth is enabled and a device is found. ");
      console.log(device);
      return true;
    } catch (error) {
      console.error('Bluetooth not enabled:', error);
      return false;
    }
  }

  async function enforceBluetoothForWebAuthn() {
    const isIOS = /iPHone|iPad|iPod/.test(navigator.userAgent);
    const isAndroid = /Andriod/.test(navigator.userAgent);

    if (isIOS || isAndroid) {
      const isBluetoothEnabled = await checkBluetoothEnabled();
      if (!isBluetoothEnabled) {
        alert("Bluetooth must be enabled");
        return;
      }
    }
  }
});
