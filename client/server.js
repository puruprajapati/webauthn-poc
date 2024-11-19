const express = require('express');
const path = require('path');

const app = express();
const PORT = 3000;

// Middleware to parse JSON requests
app.use(express.json());

// Serve static files (HTML, CSS, JS)
app.use(express.static(path.join(__dirname)));

// In-memory database for users
const users = [];

// Route to serve the dashboard
app.get('/dashboard', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'dashboard.html'));
});

// Registration endpoint
app.post('/register', (req, res) => {
  const { userId, username, displayName, phone } = req.body;

  if (users.find(user => user.userId === userId)) {
    return res.status(400).json({ message: "User already exists!" });
  }

  users.push({ userId, username, displayName, phone });
  res.status(201).json({ message: "User registered successfully!" });
});

// Login endpoint
app.post('/login', (req, res) => {
  const { loginId } = req.body;

  const user = users.find(user => user.userId === loginId);
  if (!user) {
    return res.status(404).json({ message: "User not found!" });
  }

  res.status(200).json({ message: `Welcome back, ${user.displayName}!` });
});

// Start server
app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});
