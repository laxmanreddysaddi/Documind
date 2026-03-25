const BASE_URL = process.env.REACT_APP_API_URL;

// Helper
const getToken = () => localStorage.getItem("token");

// ✅ Login
export const login = async (username, password) => {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: "POST",
    headers: {"Content-Type": "application/json"},
    body: JSON.stringify({ username, password })
  });

  if (!res.ok) throw new Error("Login failed");

  return res.json();
};

// ✅ Register
export const register = async (username, password) => {
  const res = await fetch(`${BASE_URL}/auth/register`, {
    method: "POST",
    headers: {"Content-Type": "application/json"},
    body: JSON.stringify({ username, password })
  });

  if (!res.ok) throw new Error("Register failed");

  return res.text();
};

// ✅ Chat (UPDATED)
export const askQuestion = async (question, username) => {
  const res = await fetch(
    `${BASE_URL}/chat?question=${encodeURIComponent(question)}&username=${username}`,
    {
      headers: {
        Authorization: `Bearer ${getToken()}`
      }
    }
  );

  if (!res.ok) throw new Error("Chat failed");

  return res.text();
};

// ✅ Upload
export const uploadFile = async (file) => {
  const formData = new FormData();
  formData.append("file", file);

  const res = await fetch(`${BASE_URL}/documents/upload`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${getToken()}`
    },
    body: formData
  });

  if (!res.ok) throw new Error("Upload failed");

  return res.text();
};

// ✅ Chat history (UPDATED)
export const getHistory = async (username) => {
  const res = await fetch(`${BASE_URL}/history/${username}`, {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  });

  if (!res.ok) throw new Error("History fetch failed");

  return res.json();
};

// ✅ Documents
export const getDocuments = async () => {
  const res = await fetch(`${BASE_URL}/documents/history`, {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  });

  if (!res.ok) throw new Error("Documents fetch failed");

  return res.json();
};

// ✅ Clear chat
export const clearHistory = async (username) => {
  const res = await fetch(`${BASE_URL}/history/${username}`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  });

  if (!res.ok) throw new Error("Clear history failed");
};