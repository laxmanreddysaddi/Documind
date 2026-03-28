import { useState, useEffect, useRef } from "react";
import axios from "axios";

const BASE_URL =
  process.env.REACT_APP_API_URL ||
  "https://documind-backend-30m4.onrender.com/api";

// =========================
// ✅ AXIOS INSTANCE
// =========================
const api = axios.create({
  baseURL: BASE_URL,
});

// ✅ Attach token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// ✅ Auto logout on 401
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      alert("Session expired");
      localStorage.clear();
      window.location.reload();
    }
    return Promise.reject(err);
  }
);

export default function App() {

  // =========================
  // 🔐 AUTH
  // =========================
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState(localStorage.getItem("username") || "");
  const [password, setPassword] = useState("");
  const [token, setToken] = useState(localStorage.getItem("token") || "");
  const [authLoading, setAuthLoading] = useState(false);

  // =========================
  // 💬 CHAT (PER USER)
  // =========================
  const [messages, setMessages] = useState(() => {
    const user = localStorage.getItem("username");
    return JSON.parse(localStorage.getItem("chat_" + user)) || [];
  });

  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);

  // =========================
  // 📂 DOCUMENTS
  // =========================
  const [documents, setDocuments] = useState([]);
  const [uploading, setUploading] = useState(false);

  const textareaRef = useRef(null);

  // =========================
  // 🔄 AUTO LOGOUT (1hr)
  // =========================
  useEffect(() => {
    const loginTime = localStorage.getItem("loginTime");
    if (loginTime) {
      if (Date.now() - loginTime > 3600000) {
        alert("Session expired");
        localStorage.clear();
        setToken("");
      }
    }
  }, []);

  // =========================
  // 💾 SAVE CHAT PER USER
  // =========================
  useEffect(() => {
    const user = localStorage.getItem("username");
    if (user) {
      localStorage.setItem("chat_" + user, JSON.stringify(messages));
    }
  }, [messages]);

  // =========================
  // 📂 FETCH DOCUMENTS
  // =========================
  const fetchDocuments = async () => {
    try {
      const user = localStorage.getItem("username");

      const res = await api.get("/documents/history", {
        params: { username: user },
      });

      setDocuments(res.data || []);
    } catch (err) {
      console.log("❌ Document fetch error", err);
    }
  };

  useEffect(() => {
    if (token) fetchDocuments();
  }, [token]);

  // =========================
  // 🔐 AUTH
  // =========================
  const handleAuth = async () => {

    if (!username || !password) return alert("Enter credentials");

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
  // 💬 CHAT
  // =========================
  const sendMessage = async () => {

    if (!question.trim()) return;

    const userMsg = { role: "user", text: question };
    setMessages((prev) => [...prev, userMsg]);

    const q = question;
    setQuestion("");
    setLoading(true);

    try {
      const res = await api.get("/chat", {
        params: {
          question: q,
          username: localStorage.getItem("username"),
        },
      });

      setMessages((prev) => [
        ...prev,
        { role: "ai", text: res.data },
      ]);

    } catch {
      setMessages((prev) => [
        ...prev,
        { role: "ai", text: "❌ Error" },
      ]);
    }

    setLoading(false);
  };

  // =========================
  // 📤 UPLOAD
  // =========================
  const uploadFile = async (e) => {

    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);
    formData.append("username", localStorage.getItem("username"));

    try {
      setUploading(true);

      await api.post("/documents/upload", formData);

      alert("✅ Uploaded");
      fetchDocuments();

    } catch {
      alert("❌ Upload failed");
    } finally {
      setUploading(false);
    }
  };

  // =========================
  // ❌ DELETE DOCUMENT
  // =========================
  const deleteDoc = async (id) => {
    if (!window.confirm("Delete document?")) return;

    await api.delete(`/documents/delete/${id}`);
    fetchDocuments();
  };

  // =========================
  // 🧹 CLEAR CHAT
  // =========================
  const clearChat = () => {
    const user = localStorage.getItem("username");
    localStorage.removeItem("chat_" + user);
    setMessages([]);
  };

  // =========================
  // 🚪 LOGOUT
  // =========================
  const logout = () => {
    localStorage.clear();
    setToken("");
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
              {authLoading ? "Wait..." : isLogin ? "Login" : "Register"}
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
  // 💬 MAIN UI
  // =========================
  return (
    <div className="flex h-screen bg-gray-900 text-white">

      {/* SIDEBAR */}
      <div className="w-64 bg-black p-4 flex flex-col">

        <button onClick={clearChat} className="bg-blue-600 p-2 mb-3">
          New Chat
        </button>

        <input type="file" onChange={uploadFile} />
        {uploading && <p>Uploading...</p>}

        <div className="mt-4">
          <h3 className="mb-2 font-bold">Documents</h3>

          {documents.map((d) => (
            <div key={d.id} className="flex justify-between mb-1">
              <span>📄 {d.fileName}</span>
              <button
                onClick={() => deleteDoc(d.id)}
                className="text-red-400 text-xs"
              >
                ❌
              </button>
            </div>
          ))}
        </div>

        <button onClick={logout} className="mt-auto bg-red-600 p-2">
          Logout
        </button>

      </div>

      {/* CHAT */}
      <div className="flex-1 flex flex-col">

        <div className="flex-1 p-4 overflow-y-auto">
          {messages.map((m, i) => (
            <div key={i} className={m.role === "user" ? "text-right" : ""}>
              {m.text}
            </div>
          ))}
          {loading && <p>Thinking...</p>}
        </div>

        <div className="p-3 flex">
          <textarea
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
          <button onClick={sendMessage} className="bg-blue-600 px-4">
            Send
          </button>
        </div>

      </div>
    </div>
  );
}