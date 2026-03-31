import { useState, useEffect, useRef } from "react";
import axios from "axios";

const BASE_URL =
  process.env.REACT_APP_API_URL ||
  "https://documind-backend-30m4.onrender.com/api";

const api = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

// ✅ Attach token
api.interceptors.request.use((config) => {
  const currentUser = localStorage.getItem("currentUser");
  const token = currentUser
    ? localStorage.getItem(`token_${currentUser}`)
    : null;

  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default function App() {

  // ================= AUTH =================
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [token, setToken] = useState("");
  const [authLoading, setAuthLoading] = useState(false);

  // ================= DATA =================
  const [documents, setDocuments] = useState([]);
  const [selectedDocId, setSelectedDocId] = useState("");

  const [sessions, setSessions] = useState([]);
  const [selectedSessionId, setSelectedSessionId] = useState("");

  const [messages, setMessages] = useState([]);
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);

  const [uploading, setUploading] = useState(false);

  const chatEndRef = useRef(null);

  // ================= BACKEND WAKE =================
  useEffect(() => {
    fetch(`${BASE_URL}/auth/login`, { method: "OPTIONS" }).catch(() => {});
  }, []);

  // ================= AUTO SCROLL =================
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // ================= FETCH DOCUMENTS =================
  const fetchDocuments = async () => {
    if (!username) return;

    try {
      const res = await api.get("/documents/history", {
        params: { username },
      });
      setDocuments(res.data || []);
    } catch {}
  };

  useEffect(() => {
    if (token) fetchDocuments();
  }, [token]);

  // ================= FILE UPLOAD =================
  const handleUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setUploading(true);

    const formData = new FormData();
    formData.append("file", file);
    formData.append("username", username);

    try {
      await api.post("/documents/upload", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });

      fetchDocuments();
    } catch {
      alert("Upload failed");
    }

    setUploading(false);
  };

  // ================= FETCH SESSIONS =================
  const fetchSessions = async (docId) => {
    if (!docId) return;

    try {
      const res = await api.get("/chat/sessions", {
        params: { username, documentId: docId },
      });
      setSessions(res.data || []);
    } catch {}
  };

  // ================= FETCH CHAT =================
  const fetchChatHistory = async (sessionId) => {
    if (!sessionId) return;

    try {
      const res = await api.get("/chat/history", {
        params: { sessionId },
      });

      const formatted = res.data.flatMap((item) => [
        { role: "user", text: item.question },
        { role: "ai", text: item.answer },
      ]);

      setMessages(formatted);
    } catch {}
  };

  useEffect(() => {
    if (selectedDocId) {
      fetchSessions(selectedDocId);
      setMessages([]);
      setSelectedSessionId("");
    }
  }, [selectedDocId]);

  useEffect(() => {
    if (selectedSessionId) {
      fetchChatHistory(selectedSessionId);
    }
  }, [selectedSessionId]);

  // ================= AUTH =================
  const handleAuth = async () => {
    if (!username || !password) {
      alert("Enter username & password");
      return;
    }

    setAuthLoading(true);

    try {
      const url = isLogin ? "/auth/login" : "/auth/register";
      const res = await api.post(url, { username, password });

      if (isLogin) {
        const t = res.data.token || res.data;

        setToken(t);
        localStorage.setItem(`token_${username}`, t);
        localStorage.setItem("currentUser", username);
      } else {
        alert("Registered! Now login");
        setIsLogin(true);
      }
    } catch {
      alert("Auth failed");
    }

    setAuthLoading(false);
  };

  // ================= SEND MESSAGE =================
  const sendMessage = async () => {
    if (!question.trim()) return;

    if (!selectedDocId) {
      alert("Select document first");
      return;
    }

    const q = question;
    setQuestion("");

    setMessages((prev) => [...prev, { role: "user", text: q }]);
    setLoading(true);

    let sessionId = selectedSessionId;

    try {
      if (!sessionId) {
        const res = await api.post("/chat/session/create", null, {
          params: { username, documentId: selectedDocId, question: q },
        });

        sessionId = res.data.id;
        setSelectedSessionId(sessionId);

        fetchSessions(selectedDocId);
      }

      const res = await api.post("/chat/ask", null, {
        params: { question: q, documentId: selectedDocId, sessionId },
      });

      setMessages((prev) => [
        ...prev,
        { role: "ai", text: res.data },
      ]);

    } catch {
      setMessages((prev) => [
        ...prev,
        { role: "ai", text: "Error getting answer" },
      ]);
    }

    setLoading(false);
  };

  const logout = () => {
    localStorage.clear();
    setToken("");
    setMessages([]);
    setDocuments([]);
  };

  // ================= LOGIN =================
  if (!token) {
    return (
      <div className="flex h-screen bg-black text-white items-center justify-center">
        <div className="bg-white/10 p-6 rounded w-80">

          <h2 className="text-xl mb-4 text-center">
            {isLogin ? "Login" : "Register"}
          </h2>

          <input
            className="w-full p-2 mb-3 bg-white/20"
            placeholder="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />

          <input
            type="password"
            className="w-full p-2 mb-3 bg-white/20"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <button
            onClick={handleAuth}
            className="w-full bg-blue-600 p-2"
          >
            {authLoading
              ? "Please wait..."
              : isLogin
              ? "Login"
              : "Register"}
          </button>

          <p
            className="text-sm mt-3 text-center cursor-pointer text-blue-300"
            onClick={() => setIsLogin(!isLogin)}
          >
            Toggle Login/Register
          </p>

        </div>
      </div>
    );
  }

  // ================= MAIN UI =================
  return (
    <div className="flex h-screen bg-black text-white">

      {/* LEFT PANEL */}
      <div className="w-64 p-3 bg-white/10 flex flex-col">

        {/* Upload */}
        <label className="bg-gray-700 p-2 text-center cursor-pointer mb-2">
          {uploading ? "Uploading..." : "Choose File"}
          <input type="file" hidden onChange={handleUpload} />
        </label>

        {/* New Chat */}
        <button
          onClick={() => {
            setSelectedSessionId("");
            setMessages([]);
          }}
          className="bg-blue-600 w-full p-2 mb-3"
        >
          + New Chat
        </button>

        {/* Documents */}
        {documents.map((d) => (
          <div
            key={d.id}
            onClick={() => {
              setSelectedDocId(d.id);
              setSelectedSessionId("");
              fetchSessions(d.id);
            }}
            className={`p-2 cursor-pointer flex justify-between ${
              selectedDocId === d.id
                ? "bg-blue-600"
                : "hover:bg-gray-700"
            }`}
          >
            <span>{d.fileName}</span>
            <button
              onClick={(e) => {
                e.stopPropagation();
                deleteDocument(d.id);
              }}
            >
              🗑
            </button>
          </div>
        ))}

        {/* Sessions */}
        {sessions.map((s) => (
          <div
            key={s.id}
            onClick={() => setSelectedSessionId(s.id)}
            className="p-2 hover:bg-gray-700 cursor-pointer"
          >
            {s.name}
          </div>
        ))}

        <button onClick={logout} className="bg-red-600 mt-auto p-2">
          Logout
        </button>
      </div>

      {/* CHAT */}
      <div className="flex-1 flex flex-col">

        <div className="flex-1 p-4 overflow-y-auto">
          {!selectedDocId && (
            <p className="text-gray-400">
              👉 Select a document to start chat
            </p>
          )}

          {messages.map((m, i) => (
            <div key={i}>{m.text}</div>
          ))}

          {loading && <p>Thinking...</p>}

          <div ref={chatEndRef}></div>
        </div>

        <div className="p-3 flex gap-2">
          <input
            className="flex-1 p-2 bg-white/20"
            placeholder="Ask something..."
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
          />
          <button onClick={sendMessage} className="bg-blue-600 px-4">
            Send
          </button>
        </div>

      </div>
    </div>
  );
}