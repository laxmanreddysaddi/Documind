import { useState, useEffect, useRef } from "react";
import axios from "axios";

const BASE_URL =
  process.env.REACT_APP_API_URL ||
  "https://documind-backend-30m4.onrender.com/api";

const api = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

// 🔥 USE SESSION STORAGE (FIX USER LEAK ISSUE)
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

  // ================= 🔥 WAKE BACKEND =================
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

  // ================= DOC CHANGE =================
  useEffect(() => {
    if (selectedDocId) {
      fetchSessions(selectedDocId);
      setMessages([]);
      setSelectedSessionId("");
    }
  }, [selectedDocId]);

  // ================= SESSION CHANGE =================
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

  // ================= DELETE DOCUMENT =================
  const deleteDocument = async (id) => {
    if (!window.confirm("Delete this document?")) return;

    try {
      await api.delete(`/documents/delete/${id}`);

      if (selectedDocId == id) {
        setSelectedDocId("");
        setMessages([]);
        setSessions([]);
      }

      fetchDocuments();
    } catch {
      alert("Delete failed");
    }
  };

  // ================= AUTH =================
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
        const t = res.data.token || res.data;
        setToken(t);

        // 🔥 SESSION STORAGE
        sessionStorage.setItem("token", t);
        sessionStorage.setItem("username", username);

      } else {
        alert("Registered successfully! Now login");
        setIsLogin(true);
      }

    } catch {
      alert("Auth failed");
    } finally {
      setAuthLoading(false);
    }
  };

  // ================= CHAT =================
  const sendMessage = async () => {
    if (!question.trim()) return;

    if (!selectedDocId) {
      alert("Select document first");
      return;
    }

    let sessionId = selectedSessionId;
    const q = question;

    if (!sessionId) {
      try {
        const res = await api.post("/chat/session/create", null, {
          params: {
            username,
            documentId: selectedDocId,
            question: q,
          },
        });

        sessionId = res.data.id;
        setSelectedSessionId(sessionId);

        const cleanName = q.replace(/\s+/g, " ").trim().substring(0, 30);

        setSessions((prev) => [
          { id: sessionId, name: cleanName },
          ...prev,
        ]);

        setTimeout(() => fetchSessions(selectedDocId), 300);

      } catch {
        alert("Failed to create session");
        return;
      }
    }

    setQuestion("");
    setMessages((prev) => [...prev, { role: "user", text: q }]);
    setLoading(true);

    try {
      const res = await api.post("/chat/ask", null, {
        params: {
          question: q,
          documentId: selectedDocId,
          sessionId: sessionId,
        },
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

  // ================= ENTER =================
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

    try {
      setUploading(true);
      setUploadProgress(0);

      await api.post("/documents/upload", formData, {
        onUploadProgress: (progressEvent) => {
          const percent = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          );
          setUploadProgress(percent);
        },
      });

      fetchDocuments();

    } catch {
      alert("Upload failed");
    } finally {
      setUploading(false);
      setUploadProgress(0);
    }
  };

  // ================= LOGOUT =================
  const logout = () => {
    sessionStorage.clear();
    setToken("");
    setMessages([]);
    setDocuments([]);
  };

  // ================= LOGIN UI =================
  if (!token) {
    return (
      <div className="flex h-screen bg-gradient-to-br from-black to-gray-900">
        <div className="w-1/2 flex items-center justify-center text-white text-4xl font-bold">
          DocuMind AI
        </div>

        <div className="w-1/2 flex items-center justify-center">
          <div className="backdrop-blur-xl bg-white/10 p-8 rounded-xl w-80 text-white">
            <h2 className="text-2xl font-bold mb-6 text-center">
              {isLogin ? "Login" : "Register"}
            </h2>

            <input className="w-full p-2 mb-4 rounded bg-white/20"
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />

            <input type="password"
              className="w-full p-2 mb-4 rounded bg-white/20"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />

            <button
              onClick={handleAuth}
              disabled={authLoading}
              className="w-full bg-blue-600 p-2 rounded"
            >
              {authLoading
                ? "Please wait..."
                : isLogin
                ? "Login"
                : "Register"}
            </button>

            <p
              className="text-sm text-center mt-4 cursor-pointer text-blue-300"
              onClick={() => setIsLogin(!isLogin)}
            >
              {isLogin
                ? "New user? Register"
                : "Already have account? Login"}
            </p>
          </div>
        </div>
      </div>
    );
  }

  // ================= MAIN UI =================
  return (
    <div className="flex h-screen bg-gradient-to-br from-black to-gray-900 text-white">

      {/* SIDEBAR */}
      <div className="w-64 p-4 flex flex-col backdrop-blur-xl bg-white/10 border-r border-white/20">

        <button onClick={createSession}
          className="bg-blue-600 p-2 rounded mb-3">
          + New Chat
        </button>

        <input type="file" onChange={uploadFile} />

        {uploading && (
          <div className="mt-3">
            <div className="text-xs text-blue-300 mb-1">
              Uploading {uploadProgress}%
            </div>
            <div className="w-full h-2 bg-white/20 rounded">
              <div className="h-2 bg-blue-500 rounded"
                style={{ width: `${uploadProgress}%` }} />
            </div>
          </div>
        )}

        <div className="mt-3 space-y-2">
          {documents.map((d) => (
            <div key={d.id}
              className={`p-2 rounded flex justify-between ${
                selectedDocId == d.id ? "bg-blue-500/40" : "bg-white/10"
              }`}>
              <span onClick={() => setSelectedDocId(d.id)}>
                {d.fileName}
              </span>
              <button onClick={() => deleteDocument(d.id)}>🗑</button>
            </div>
          ))}
        </div>

        <button onClick={logout}
          className="mt-auto bg-red-600 p-2 rounded">
          Logout
        </button>
      </div>

      {/* CHAT */}
      <div className="flex-1 flex flex-col">
        <div className="flex-1 p-4 overflow-y-auto">
          {messages.map((m, i) => (
            <div key={i} className="p-2 mb-2 bg-white/10 rounded">
              {m.text}
            </div>
          ))}
          {loading && <p>Thinking...</p>}
          <div ref={chatEndRef}></div>
        </div>

        <div className="p-3 flex gap-2 bg-white/10">
          <textarea
            className="flex-1 p-2 rounded bg-white/20"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={handleKeyDown}
          />
          <button onClick={sendMessage}
            className="bg-blue-600 px-4 rounded">
            Send
          </button>
        </div>
      </div>
    </div>
  );
}