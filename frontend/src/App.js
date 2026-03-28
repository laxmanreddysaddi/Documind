import { useState, useEffect, useRef } from "react";
import axios from "axios";

const BASE_URL =
  process.env.REACT_APP_API_URL ||
  "https://documind-backend-30m4.onrender.com/api";

// ✅ AXIOS INSTANCE WITH AUTO LOGOUT
const api = axios.create({
  baseURL: BASE_URL,
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response && err.response.status === 401) {
      alert("Session expired. Please login again.");
      localStorage.clear();
      window.location.reload();
    }
    return Promise.reject(err);
  }
);

export default function App() {

  // 🔐 AUTH
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState(localStorage.getItem("username") || "");
  const [password, setPassword] = useState("");
  const [token, setToken] = useState(localStorage.getItem("token") || "");
  const [authLoading, setAuthLoading] = useState(false);

  // 💬 CHAT
  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);

  // 📤 UPLOAD
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);

  // 📂 DOCUMENTS
  const [documents, setDocuments] = useState([]);

  const textareaRef = useRef(null);
  const chatEndRef = useRef(null);

  // =========================
  // 🔄 SESSION EXPIRY CHECK
  // =========================
  useEffect(() => {
    const loginTime = localStorage.getItem("loginTime");

    if (loginTime) {
      const now = Date.now();
      const ONE_HOUR = 60 * 60 * 1000;

      if (now - loginTime > ONE_HOUR) {
        alert("Session expired. Please login again.");
        localStorage.clear();
        setToken("");
      }
    }
  }, []);

  // 🔽 Auto scroll
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, loading]);

  // 🔄 Auto resize textarea
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
      textareaRef.current.style.height =
        textareaRef.current.scrollHeight + "px";
    }
  }, [question]);

  // =========================
  // 🔐 AUTH
  // =========================
  const handleAuth = async () => {
    if (!username || !password) {
      alert("Enter username & password");
      return;
    }

    try {
      setAuthLoading(true);

      const url = isLogin ? "/auth/login" : "/auth/register";

      const res = await api.post(url, { username, password });

      if (isLogin) {
        const tokenValue = res.data.token || res.data;

        setToken(tokenValue);
        localStorage.setItem("token", tokenValue);
        localStorage.setItem("username", username);
        localStorage.setItem("loginTime", Date.now()); // 🔥 SAVE TIME
      } else {
        alert("✅ Registered! Now login");
        setIsLogin(true);
      }

    } catch (err) {
      alert("❌ " + (err.response?.data || err.message));
    } finally {
      setAuthLoading(false);
    }
  };

  // =========================
  // 📂 FETCH DOCUMENTS
  // =========================
  const fetchDocuments = async () => {
    try {
      const res = await api.get("/documents/history", {
        params: { username },
        headers: { Authorization: `Bearer ${token}` }
      });

      setDocuments(res.data || []);
    } catch {
      console.log("Failed to fetch documents");
    }
  };

  useEffect(() => {
    if (token) fetchDocuments();
  }, [token]);

  // =========================
  // 💬 SEND MESSAGE
  // =========================
  const sendMessage = async () => {
    if (!question.trim()) return;

    const userMsg = { role: "user", text: question };
    setMessages((prev) => [...prev, userMsg]);

    setQuestion("");
    setLoading(true);

    try {
      const res = await api.get("/chat", {
        params: { question, username },
        headers: { Authorization: `Bearer ${token}` }
      });

      setMessages((prev) => [
        ...prev,
        { role: "ai", text: res.data },
      ]);

    } catch {
      setMessages((prev) => [
        ...prev,
        { role: "ai", text: "❌ Error getting response" },
      ]);
    }

    setLoading(false);
  };

  // =========================
  // 📤 FILE UPLOAD
  // =========================
  const uploadFile = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);
    formData.append("username", username);

    try {
      setUploading(true);
      setProgress(0);

      await api.post("/documents/upload", formData, {
  onUploadProgress: (event) => {
    const percent = Math.round((event.loaded * 100) / event.total);
    setProgress(percent);
  },
});

      alert("✅ Uploaded successfully");
      fetchDocuments();

    } catch {
      alert("❌ Upload failed");
    } finally {
      setUploading(false);
    }
  };

  // =========================
  // 🔐 LOGIN UI
  // =========================
  if (!token) {
    return (
      <div className="flex h-screen">

        <div className="w-1/2 bg-gradient-to-br from-blue-600 to-purple-600 text-white flex justify-center items-center">
          <h1 className="text-4xl font-bold">DocuMind AI</h1>
        </div>

        <div className="w-1/2 flex justify-center items-center bg-gray-100">
          <div className="bg-white p-8 rounded-xl shadow-xl w-96">

            <h2 className="text-2xl font-bold mb-4">
              {isLogin ? "Login" : "Register"}
            </h2>

            <input
              className="w-full border p-2 mb-3 rounded"
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />

            <input
              type="password"
              className="w-full border p-2 mb-3 rounded"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />

            <button
              onClick={handleAuth}
              disabled={authLoading}
              className="w-full p-2 rounded text-white bg-blue-600"
            >
              {authLoading ? "Please wait..." : isLogin ? "Login" : "Register"}
            </button>

            <p
              className="mt-3 text-blue-600 cursor-pointer"
              onClick={() => setIsLogin(!isLogin)}
            >
              {isLogin
                ? "Don't have account? Register"
                : "Already have account? Login"}
            </p>

          </div>
        </div>
      </div>
    );
  }

  // =========================
  // 💬 CHAT UI
  // =========================
  return (
    <div className="flex h-screen bg-[#0f172a]">

      {/* SIDEBAR */}
      <div className="w-64 bg-[#020617] text-white p-4 flex flex-col">

        <h2 className="text-xl font-bold mb-4">🤖 DocuMind</h2>

        <button
          className="bg-blue-600 p-2 rounded mb-3 w-full"
          onClick={() => setMessages([])}
        >
          + New Chat
        </button>

        {/* Upload */}
        <label className="cursor-pointer bg-gray-500 p-2 rounded w-full text-center block">
          {uploading ? `Uploading... ${progress}%` : "Upload File"}

          <input
            type="file"
            className="hidden"
            onChange={uploadFile}
            disabled={uploading}
          />
        </label>

        {/* Progress */}
        {uploading && (
          <div className="mt-3 w-full bg-gray-700 rounded">
            <div
              className="bg-blue-500 h-2 rounded"
              style={{ width: `${progress}%` }}
            />
          </div>
        )}

        {/* 📂 DOCUMENTS */}
        <div className="mt-6 flex-1 overflow-y-auto">
          <h3 className="text-sm text-gray-400 mb-2">📂 Documents</h3>

          {documents.length === 0 ? (
            <p className="text-gray-500 text-sm">No documents</p>
          ) : (
            documents.map((doc, i) => (
              <div key={i} className="bg-gray-800 p-2 rounded mb-2 text-sm">
                📄 {doc.fileName}
              </div>
            ))
          )}
        </div>

        <button
          className="mt-4 w-full bg-red-600 p-2 rounded"
          onClick={() => {
            localStorage.clear();
            setToken("");
          }}
        >
          Logout
        </button>
      </div>

      {/* CHAT */}
      <div className="flex flex-col flex-1">

        <div className="flex-1 p-6 overflow-y-auto">
          {messages.map((msg, i) => (
            <div key={i} className={`mb-4 flex ${msg.role === "user" ? "justify-end" : "justify-start"}`}>
              <div className={`max-w-xl px-4 py-3 rounded-2xl ${
                msg.role === "user"
                  ? "bg-blue-600 text-white"
                  : "bg-gray-800 text-white"
              }`}>
                {msg.text}
              </div>
            </div>
          ))}

          {loading && <div className="text-gray-400">🤖 Thinking...</div>}

          <div ref={chatEndRef} />
        </div>

        <div className="p-4 flex">
          <textarea
            ref={textareaRef}
            className="flex-1 p-3 rounded-xl resize-none"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
              }
            }}
          />

          <button
            onClick={sendMessage}
            className="ml-3 bg-blue-600 px-5 py-2 rounded-xl text-white"
          >
            Send
          </button>
        </div>
      </div>
    </div>
  );
}