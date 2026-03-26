import { useState, useEffect, useRef } from "react";
import axios from "axios";

// ✅ BASE URL (ENV + FALLBACK)
const BASE_URL =
  process.env.REACT_APP_API_URL ||
  "https://documind-backend-30m4.onrender.com/api";

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

  const textareaRef = useRef(null);
  const chatEndRef = useRef(null);

  // 🔥 Wake backend (reduce delay)
  useEffect(() => {
    fetch(`${BASE_URL}/chat?question=hi&username=test`).catch(() => {});
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

  // 🔐 AUTH
  const handleAuth = async () => {
    if (!username || !password) {
      alert("Enter username & password");
      return;
    }

    try {
      setAuthLoading(true);

      const url = isLogin
        ? `${BASE_URL}/auth/login`
        : `${BASE_URL}/auth/register`;

      const res = await axios.post(url, { username, password });

      if (isLogin) {
        const tokenValue = res.data.token || res.data;

        setToken(tokenValue);
        localStorage.setItem("token", tokenValue);
        localStorage.setItem("username", username);

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

  // 💬 SEND MESSAGE
  const sendMessage = async () => {
    if (!question.trim()) return;

    const userMsg = { role: "user", text: question };
    setMessages((prev) => [...prev, userMsg]);
    setQuestion("");
    setLoading(true);

    try {
      const res = await axios.get(`${BASE_URL}/chat`, {
        params: { question, username },
        headers: { Authorization: `Bearer ${token}` },
      });

      setMessages((prev) => [
        ...prev,
        { role: "ai", text: res.data },
      ]);

    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { role: "ai", text: "❌ Error getting response" },
      ]);
    }

    setLoading(false);
  };

  // 📤 FILE UPLOAD
  const uploadFile = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
      alert("❌ File too large (max 5MB)");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);

    try {
      setUploading(true);
      setProgress(0);

      await axios.post(
        `${BASE_URL}/documents/upload`,
        formData,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
          onUploadProgress: (event) => {
            const percent = Math.round(
              (event.loaded * 100) / event.total
            );
            setProgress(percent);
          },
        }
      );

      alert("✅ Uploaded! Processing document...");

    } catch (err) {
      alert("❌ Upload failed");
    } finally {
      setUploading(false);
    }
  };

  // 🔐 LOGIN UI
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
              className={`w-full p-2 rounded text-white ${
                authLoading ? "bg-gray-400" : "bg-blue-600"
              }`}
            >
              {authLoading
                ? isLogin
                  ? "Logging in..."
                  : "Registering..."
                : isLogin
                ? "Login"
                : "Register"}
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

  // 💬 CHAT UI
  return (
    <div className="flex h-screen bg-[#0f172a]">

      {/* SIDEBAR */}
      <div className="w-64 bg-[#020617] text-white p-4">

        <h2 className="text-xl font-bold mb-4">🤖 DocuMind</h2>

        <button
          className="bg-blue-600 text-white p-2 rounded mb-3 w-full"
          onClick={() => setMessages([])}
        >
          + New Chat
        </button>

        {/* Upload */}
        <label className="cursor-pointer bg-gray-500 text-white p-2 rounded w-full text-center block">
          {uploading ? `Uploading... ${progress}%` : "Upload File"}

          <input
            type="file"
            className="hidden"
            onChange={uploadFile}
            disabled={uploading}
          />
        </label>

        {/* Progress Bar */}
        {uploading && (
          <div className="mt-3 w-full bg-gray-700 rounded">
            <div
              className="bg-blue-500 h-2 rounded"
              style={{ width: `${progress}%` }}
            />
          </div>
        )}

        <button
          className="mt-4 w-full bg-red-600 text-white p-2 rounded"
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

        {/* Messages */}
        <div className="flex-1 p-6 overflow-y-auto">
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
                    : "bg-gray-800 text-white"
                }`}
              >
                {msg.text}
              </div>
            </div>
          ))}

          {loading && (
            <div className="text-gray-400">🤖 Thinking...</div>
          )}

          <div ref={chatEndRef} />
        </div>

        {/* Input */}
        <div className="p-4 flex">

          <textarea
            ref={textareaRef}
            className="flex-1 p-3 rounded-xl resize-none"
            value={question}
            placeholder="Ask about your document..."
            onChange={(e) => setQuestion(e.target.value)}

            // ✅ ENTER TO SEND
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
              }
            }}
          />

          <button
            onClick={sendMessage}
            className="ml-3 bg-blue-600 text-white px-5 py-2 rounded-xl"
          >
            Send
          </button>

        </div>
      </div>
    </div>
  );
}