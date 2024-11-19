document.addEventListener("DOMContentLoaded", () => {
  // Get the username from the localStorage or sessionStorage if available (assuming you set it during login)
  const username = sessionStorage.getItem("username") || "User";

  // Display the username on the dashboard
  document.getElementById("username-display").innerText = username;

  // Logout functionality
  const logoutButton = document.getElementById("logout-btn");

  logoutButton.addEventListener("click", () => {
    // Remove user data from session or local storage and redirect to the login page
    sessionStorage.removeItem("username");
    window.location.href = "/login"; // Redirect to login page after logout
  });
});
