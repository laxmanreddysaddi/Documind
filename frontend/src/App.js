import { useState, useEffect, useRef } from "react";
import axios from "axios";

const BASE_URL =
  process.env.REACT_APP_API_URL ||
  "https://documind-backend-30m4.onrender.com/api";

const api = axios.create({
  baseURL: BASE_URL,
});

// ✅ AUTO LOGOUT
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

  // =========================
  // 🔄 SESSION EXPIRY
  // =========================
  useEffect(() => {
    const loginTime = localStorage.getItem("loginTime");
    if (loginTime) {
      const now = Date.now();
      if (now - loginTime > 3600000) {
        alert("Session expired");
        localStorage.clear();
        setToken("");
      }
    }
  }, []);

  // =========================
  // 📂 FETCH DOCUMENTS
  // =========================
  const fetchDocuments = async () => {
    try {
      const res = await api.get("/documents/history", {
        params: {
          username: localStorage.getItem("username") // ✅ FIX
        },
        headers: { Authorization: `Bearer ${token}` }
      });

      setDocuments(res.data || []);

    } catch (e) {
      console.log(e);
    }
  };

  useEffect(() => {
    if (token) fetchDocuments();
  }, [token]);

  // =========================
  // 🔐 AUTH
  // =========================
  const handleAuth = async () => {
    if (!username || !password) {
      alert("Enter credentials");
      return;
    }

    try {
      setAuthLoading(true);

      const url = isLogin ? "/auth/login" : "/auth/register";
      const res = await api.post(url, { username, password });

      if (isLogin) {
        const t = res.data.token || res.data;

        setToken(t);
        localStorage.setItem("token", t);
        localStorage.setItem("username", username);
        localStorage.setItem("loginTime", Date.now());
      } else {
        alert("Registered! Login now");
        setIsLogin(true);
      }

    } catch (e) {
      alert("❌ " + (e.response?.data || e.message));
    } finally {
      setAuthLoading(false);
    }
  };

  // =========================
  // 💬 SEND MESSAGE
  // =========================
  const sendMessage = async () => {
    if (!question.trim()) return;

    setMessages((prev) => [...prev, { role: "user", text: question }]);
    setQuestion("");
    setLoading(true);

    try {
      const res = await api.get("/chat", {
        params: {
          question,
          username: localStorage.getItem("username") // ✅ FIX
        },
        headers: { Authorization: `Bearer ${token}` }
      });

      setMessages((prev) => [
        ...prev,
        { role: "ai", text: res.data },
      ]);

    } catch (e) {
      console.log(e);
      setMessages((prev) => [
        ...prev,
        { role: "ai", text: "❌ Error getting response" },
      ]);
    }

    setLoading(false);
  };

  // =========================
  // 📤 FILE UPLOAD (FINAL FIX)
  // =========================
  const uploadFile = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);

    try {
      setUploading(true);
      setProgress(0);

      await api.post("/documents/upload", formData, {
        params: {
          username: localStorage.getItem("username") // ✅ FIX
        },
        headers: {
          Authorization: `Bearer ${token}`
        },
        onUploadProgress: (event) => {
          const percent = Math.round((event.loaded * 100) / event.total);
          setProgress(percent);
        }
      });

      alert("✅ Uploaded successfully");
      fetchDocuments();

    } catch (e) {
      console.log(e);
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

        <div className="w-1/2 bg-blue-600 text-white flex justify-center items-center">
          <h1 className="text-4xl font-bold">DocuMind AI</h1>
        </div>

        <div className="w-1/2 flex justify-center items-center bg-gray-100">
          <div className="bg-white p-8 rounded-xl w-96">

            <h2 className="text-xl mb-4">
              {isLogin ? "Login" : "Register"}
            </h2>

            <input
              className="w-full border p-2 mb-2"
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />

            <input
              type="password"
              className="w-full border p-2 mb-2"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />

            <button
              onClick={handleAuth}
              className="w-full bg-blue-600 text-white p-2"
            >
              {authLoading ? "Please wait..." : isLogin ? "Login" : "Register"}
            </button>

            <p
              className="mt-2 text-blue-600 cursor-pointer"
              onClick={() => setIsLogin(!isLogin)}
            >
              Switch
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
    <div className="flex h-screen bg-gray-900 text-white">

      {/* SIDEBAR */}
      <div className="w-64 bg-black p-4 flex flex-col">

        <button
          onClick={() => setMessages([])}
          className="bg-blue-600 p-2 mb-3"
        >
          New Chat
        </button>

        <input type="file" onChange={uploadFile} />

        {uploading && <p>{progress}%</p>}

        <div className="mt-4">
          {documents.map((doc, i) => (
            <div key={i}>📄 {doc.fileName}</div>
          ))}
        </div>

        <button
          className="mt-auto bg-red-600 p-2"
          onClick={() => {
            localStorage.clear();
            setToken("");
          }}
        >
          Logout
        </button>
      </div>

      {/* CHAT */}
      <div className="flex-1 flex flex-col">

        <div className="flex-1 p-4 overflow-y-auto">
          {messages.map((msg, i) => (
            <div key={i} className={msg.role === "user" ? "text-right" : ""}>
              {msg.text}
            </div>
          ))}

          {loading && <p>🤖 Thinking...</p>}
        </div>

        <div className="p-3 flex">
          <textarea
            ref={textareaRef}
            className="flex-1 p-2 text-black"
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
            className="bg-blue-600 px-4 ml-2"
          >
            Send
          </button>
        </div>
      </div>
    </div>
  );
}