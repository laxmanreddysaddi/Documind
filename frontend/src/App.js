import { useState, useEffect, useRef } from "react";
import axios from "axios";

const BASE_URL =
  process.env.REACT_APP_API_URL ||
  "https://documind-backend-30m4.onrender.com/api";

const api = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default function App() {

  // ================= AUTH =================
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState(localStorage.getItem("username") || "");
  const [password, setPassword] = useState("");
  const [token, setToken] = useState(localStorage.getItem("token") || "");

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
    try {
      const url = isLogin ? "/auth/login" : "/auth/register";
      const res = await api.post(url, { username, password });

      if (isLogin) {
        const t = res.data.token || res.data;
        setToken(t);
        localStorage.setItem("token", t);
        localStorage.setItem("username", username);
      } else {
        alert("Registered! Login now");
        setIsLogin(true);
      }
    } catch {
      alert("Auth failed");
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

    // 🔥 CREATE SESSION ONLY IF NOT EXISTS
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

        // 🔥 INSTANT UI UPDATE
        const cleanName = q.replace(/\s+/g, " ").trim().substring(0, 30);

        setSessions((prev) => [
          { id: sessionId, name: cleanName },
          ...prev,
        ]);

        // 🔥 SYNC BACKEND
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
      await api.post("/documents/upload", formData);
      fetchDocuments();
    } catch {
      alert("Upload failed");
    } finally {
      setUploading(false);
    }
  };

  // ================= LOGOUT =================
  const logout = () => {
    localStorage.clear();
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

            <button onClick={handleAuth}
              className="w-full bg-blue-600 p-2 rounded">
              {isLogin ? "Login" : "Register"}
            </button>
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
          className="bg-blue-600 p-2 rounded mb-3 hover:bg-blue-700">
          + New Chat
        </button>

        <input type="file" onChange={uploadFile} />

        {/* DOCUMENTS */}
        <div className="mt-3 space-y-2">
          {documents.map((d) => (
            <div key={d.id}
              className={`p-2 rounded flex justify-between items-center ${
                selectedDocId == d.id ? "bg-blue-500/40" : "bg-white/10"
              }`}
            >
              <span className="cursor-pointer flex-1"
                onClick={() => setSelectedDocId(d.id)}>
                {d.fileName}
              </span>

              <button onClick={() => deleteDocument(d.id)}
                className="text-red-400">
                🗑
              </button>
            </div>
          ))}
        </div>

        {/* SESSIONS */}
        <div className="mt-3 space-y-2">
          {sessions.map((s) => (
            <div key={s.id}
              onClick={() => setSelectedSessionId(s.id)}
              className={`p-2 rounded cursor-pointer ${
                selectedSessionId === s.id ? "bg-blue-500/40" : "bg-white/10"
              }`}
            >
              {s.name || "New Chat"}
            </div>
          ))}
        </div>

        <button onClick={logout}
          className="mt-auto bg-red-600 p-2 rounded hover:bg-red-700">
          Logout
        </button>
      </div>

      {/* CHAT */}
      <div className="flex-1 flex flex-col">

        <div className="flex-1 p-4 overflow-y-auto space-y-3">

          {messages.map((m, i) => (
            <div key={i}
              className={`p-3 rounded-xl max-w-xl ${
                m.role === "user"
                  ? "bg-blue-500/40 ml-auto"
                  : "bg-white/10"
              }`}
            >
              {m.text}
            </div>
          ))}

          {loading && <p>Thinking...</p>}
          <div ref={chatEndRef}></div>
        </div>

        <div className="p-3 flex gap-2 backdrop-blur-xl bg-white/10">

          <textarea
            className="flex-1 p-2 rounded bg-white/20 text-white resize-none"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask something..."
            rows={2}
          />

          <button onClick={sendMessage}
            className="px-6 bg-blue-600 rounded hover:bg-blue-700">
            Send
          </button>

        </div>
      </div>
    </div>
  );
}