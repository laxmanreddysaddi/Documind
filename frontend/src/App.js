import React, { useState, useEffect } from "react";
import {
  login,
  register,
  askQuestion,
  uploadFile,
  getDocuments
} from "./api";

function App() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState([]);

  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);

  // ✅ Auto login (localStorage)
  useEffect(() => {
    const user = localStorage.getItem("username");
    if (user) {
      setUsername(user);
      setIsLoggedIn(true);
      fetchDocs(user);
    }
  }, []);

  // ✅ Fetch documents
  const fetchDocs = async (user) => {
    try {
      const docs = await getDocuments(user);
      setDocuments(docs);
    } catch (e) {
      console.log(e);
    }
  };

  // ✅ LOGIN
  const handleLogin = async () => {
    setLoading(true);
    try {
      await login(username, password);
      localStorage.setItem("username", username);
      setIsLoggedIn(true);
      fetchDocs(username);
    } catch (e) {
      alert("Login failed");
    }
    setLoading(false);
  };

  // ✅ REGISTER
  const handleRegister = async () => {
    setLoading(true);
    try {
      await register(username, password);
      alert("Registered successfully");
    } catch (e) {
      alert("Register failed");
    }
    setLoading(false);
  };

  // ✅ LOGOUT (AUTO CLEAR)
  const handleLogout = () => {
    localStorage.clear();
    setIsLoggedIn(false);
    setMessages([]);
    setDocuments([]);
  };

  // ✅ ASK QUESTION
  const handleAsk = async () => {
    if (!question) return;

    setMessages([...messages, { role: "user", text: question }]);
    setLoading(true);

    try {
      const res = await askQuestion(question, username);

      setMessages((prev) => [
        ...prev,
        { role: "bot", text: res }
      ]);
    } catch (e) {
      setMessages((prev) => [
        ...prev,
        { role: "bot", text: "❌ Error getting response" }
      ]);
    }

    setQuestion("");
    setLoading(false);
  };

  // ✅ ENTER KEY SUPPORT
  const handleKeyDown = (e) => {
    if (e.key === "Enter") {
      handleAsk();
    }
  };

  // ✅ FILE UPLOAD (FAST + USERNAME FIX)
  const handleUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setUploading(true);

    try {
      await uploadFile(file, username); // 🔥 IMPORTANT
      alert("Uploaded successfully");
      fetchDocs(username);
    } catch (err) {
      alert("Upload failed");
    }

    setUploading(false);
  };

  // =========================
  // UI
  // =========================

  if (!isLoggedIn) {
    return (
      <div className="h-screen flex items-center justify-center bg-gray-100">
        <div className="bg-white p-8 rounded-xl shadow-md w-80">
          <h2 className="text-xl font-bold mb-4">DocuMind AI</h2>

          <input
            placeholder="Username"
            className="w-full mb-2 p-2 border rounded"
            onChange={(e) => setUsername(e.target.value)}
          />

          <input
            placeholder="Password"
            type="password"
            className="w-full mb-4 p-2 border rounded"
            onChange={(e) => setPassword(e.target.value)}
          />

          <button
            onClick={handleLogin}
            className="w-full bg-blue-600 text-white p-2 rounded mb-2"
          >
            {loading ? "Loading..." : "Login"}
          </button>

          <button
            onClick={handleRegister}
            className="w-full bg-green-600 text-white p-2 rounded"
          >
            Register
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen">

      {/* ================= LEFT PANEL ================= */}
      <div className="w-1/4 bg-gray-900 text-white p-4 overflow-y-auto">
        <h2 className="text-lg font-bold mb-4">📂 Documents</h2>

        {documents.length === 0 && <p>No documents</p>}

        {documents.map((doc, i) => (
          <div key={i} className="mb-2 p-2 bg-gray-700 rounded">
            {doc.fileName}
          </div>
        ))}

        <input type="file" onChange={handleUpload} className="mt-4" />

        {uploading && <p className="mt-2">Uploading...</p>}

        <button
          onClick={handleLogout}
          className="mt-6 bg-red-500 p-2 w-full rounded"
        >
          Logout
        </button>
      </div>

      {/* ================= CHAT ================= */}
      <div className="flex-1 flex flex-col">

        {/* Messages */}
        <div className="flex-1 p-6 overflow-y-auto bg-gray-100">
          {messages.map((msg, i) => (
            <div
              key={i}
              className={`mb-4 flex ${
                msg.role === "user"
                  ? "justify-end"
                  : "justify-start"
              }`}
            >
              <div
                className={`max-w-xl px-4 py-3 rounded-2xl ${
                  msg.role === "user"
                    ? "bg-blue-600 text-white"
                    : "bg-white text-black shadow"
                }`}
              >
                {msg.text}
              </div>
            </div>
          ))}

          {loading && <p>Typing...</p>}
        </div>

        {/* Input */}
        <div className="p-4 flex border-t">
          <input
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={handleKeyDown}
            className="flex-1 border p-2 rounded mr-2"
            placeholder="Ask something..."
          />

          <button
            onClick={handleAsk}
            className="bg-blue-600 text-white px-4 rounded"
          >
            Send
          </button>
        </div>
      </div>
    </div>
  );
}

export default App;