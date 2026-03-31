import { useState, useEffect, useRef } from "react";
import axios from "axios";

const BASE_URL =
  process.env.REACT_APP_API_URL ||
  "https://documind-backend-30m4.onrender.com/api";

const api = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

// 🔒 session fix
api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default function App() {

  // ================= AUTH =================
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState(sessionStorage.getItem("username") || "");
  const [password, setPassword] = useState("");
  const [token, setToken] = useState(sessionStorage.getItem("token") || "");
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
  const [uploadProgress, setUploadProgress] = useState(0);

  const chatEndRef = useRef(null);

  // ⚡ WAKE BACKEND
  useEffect(() => {
    fetch("https://documind-backend-30m4.onrender.com/api/auth/login", {
      method: "OPTIONS",
    }).catch(() => {});
  }, []);

  // ================= AUTO SCROLL =================
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // ================= FETCH DOCUMENTS =================
  const fetchDocuments = async () => {
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

  // ================= FETCH SESSIONS =================
  const fetchSessions = async (docId) => {
    try {
      const res = await api.get("/chat/sessions", {
        params: { username, documentId: docId },
      });
      setSessions(res.data || []);
    } catch {}
  };

  // ================= FETCH HISTORY =================
  const fetchChatHistory = async (sessionId) => {
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

  // ================= NEW CHAT =================
  const createSession = () => {
    setSelectedSessionId("");
    setMessages([]);
  };

  // ================= DELETE DOC =================
  const deleteDocument = async (id) => {
    if (!window.confirm("Delete?")) return;

    await api.delete(`/documents/delete/${id}`);

    if (selectedDocId === id) {
      setSelectedDocId("");
      setMessages([]);
    }

    fetchDocuments();
  };

  // ================= AUTH =================
  const handleAuth = async () => {
    if (!username || !password) return alert("Enter details");

    setAuthLoading(true);

    try {
      const url = isLogin ? "/auth/login" : "/auth/register";
      const res = await api.post(url, { username, password });

      if (isLogin) {
        const t = res.data.token || res.data;
        setToken(t);
        sessionStorage.setItem("token", t);
        sessionStorage.setItem("username", username);
      } else {
        alert("Registered! Now login");
        setIsLogin(true);
      }

    } catch {
      alert("Auth failed");
    }

    setAuthLoading(false);
  };

  // ================= CHAT FIXED =================
  const sendMessage = async () => {
    if (!question.trim()) return;
    if (!selectedDocId) return alert("Select document");

    const q = question;
    setQuestion("");

    // ✅ show immediately
    setMessages((prev) => [...prev, { role: "user", text: q }]);
    setLoading(true);

    let sessionId = selectedSessionId;

    try {
      // ✅ create session if first msg
      if (!sessionId) {
        const res = await api.post("/chat/session/create", null, {
          params: { username, documentId: selectedDocId, question: q },
        });

        sessionId = res.data.id;
        setSelectedSessionId(sessionId);

        setTimeout(() => fetchSessions(selectedDocId), 300);
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
        { role: "ai", text: "Error" },
      ]);
    }

    setLoading(false);
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  // ================= UPLOAD =================
  const uploadFile = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);
    formData.append("username", username);

    setUploading(true);

    try {
      await api.post("/documents/upload", formData, {
        onUploadProgress: (e) => {
          setUploadProgress(Math.round((e.loaded * 100) / e.total));
        },
      });

      fetchDocuments();

    } catch {
      alert("Upload failed");
    }

    setUploading(false);
    setUploadProgress(0);
  };

  const logout = () => {
    sessionStorage.clear();
    setToken("");
    setMessages([]);
  };

  // ================= LOGIN UI =================
  if (!token) {
    return (
      <div className="flex h-screen items-center justify-center bg-black text-white">
        <div className="bg-white/10 p-6 rounded w-80">
          <h2 className="text-xl mb-4 text-center">
            {isLogin ? "Login" : "Register"}
          </h2>

          <input className="w-full p-2 mb-3 bg-white/20"
            placeholder="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />

          <input type="password"
            className="w-full p-2 mb-3 bg-white/20"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <button
            onClick={handleAuth}
            className="w-full bg-blue-600 p-2"
          >
            {authLoading ? "Loading..." : isLogin ? "Login" : "Register"}
          </button>

          <p className="text-sm mt-3 text-center cursor-pointer"
            onClick={() => setIsLogin(!isLogin)}>
            {isLogin ? "New user? Register" : "Already have account? Login"}
          </p>
        </div>
      </div>
    );
  }

  // ================= MAIN UI =================
  return (
    <div className="flex h-screen bg-black text-white">

      {/* SIDEBAR */}
      <div className="w-64 p-3 bg-white/10">

        <button onClick={createSession}
          className="bg-blue-600 w-full p-2 mb-2">
          + New Chat
        </button>

        <input type="file" onChange={uploadFile} />

        {uploading && (
          <div className="text-xs mt-2">
            Uploading {uploadProgress}%
          </div>
        )}

        {documents.map((d) => (
          <div key={d.id} className="flex justify-between mt-2">
            <span onClick={() => setSelectedDocId(d.id)}>
              {d.fileName}
            </span>
            <button onClick={() => deleteDocument(d.id)}>🗑</button>
          </div>
        ))}

        <button onClick={logout}
          className="bg-red-600 mt-5 w-full p-2">
          Logout
        </button>
      </div>

      {/* CHAT */}
      <div className="flex-1 flex flex-col">

        <div className="flex-1 p-4 overflow-y-auto">
          {messages.map((m, i) => (
            <div key={i} className="mb-2">
              {m.text}
            </div>
          ))}
          {loading && <p>Thinking...</p>}
          <div ref={chatEndRef}></div>
        </div>

        <div className="p-3 flex gap-2">
          <textarea
            className="flex-1 p-2 bg-white/20"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={handleKeyDown}
          />
          <button onClick={sendMessage}
            className="bg-blue-600 px-4">
            Send
          </button>
        </div>

      </div>
    </div>
  );
}