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

  // 🔥 CHAT SESSIONS
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
    } catch (e) {
      console.log(e);
    }
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
    } catch (e) {
      console.log(e);
    }
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

    } catch (e) {
      console.log(e);
    }
  };

  // ================= DOCUMENT CHANGE =================
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

  // ================= CREATE SESSION =================
  const createSession = async () => {
    try {
      const res = await api.post("/chat/session/create", null, {
        params: { username, documentId: selectedDocId },
      });

      fetchSessions(selectedDocId);
      setSelectedSessionId(res.data.id);

    } catch {
      alert("Create session failed");
    }
  };

  // ================= RENAME SESSION =================
  const renameSession = async (id) => {
    const name = prompt("Enter new name:");
    if (!name) return;

    try {
      await api.put("/chat/session/rename", null, {
        params: { sessionId: id, name },
      });

      fetchSessions(selectedDocId);

    } catch {
      alert("Rename failed");
    }
  };

  // ================= DELETE SESSION =================
  const deleteSession = async (id) => {
    try {
      await api.delete(`/chat/session/delete/${id}`);
      fetchSessions(selectedDocId);

      if (selectedSessionId === id) {
        setMessages([]);
        setSelectedSessionId("");
      }

    } catch {
      alert("Delete failed");
    }
  };

  // ================= SEND MESSAGE =================
  const sendMessage = async () => {
    if (!question.trim()) return;

    if (!selectedSessionId) {
      alert("Create or select chat first");
      return;
    }

    const q = question;
    setQuestion("");

    setMessages((prev) => [...prev, { role: "user", text: q }]);
    setLoading(true);

    try {
      const res = await api.post("/chat/ask", null, {
        params: { question: q, sessionId: selectedSessionId },
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
    } finally {
      setUploading(false);
    }
  };

  // ================= LOGOUT =================
  const logout = () => {
    localStorage.clear();
    setToken("");
    setMessages([]);
  };

  // ================= LOGIN =================
  if (!token) {
    return (
      <div className="flex h-screen justify-center items-center">
        <div className="bg-white p-8 rounded shadow w-80">
          <input
            className="w-full p-2 border mb-3"
            placeholder="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
          <input
            type="password"
            className="w-full p-2 border mb-3"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <button onClick={handleAuth} className="w-full bg-blue-600 text-white p-2">
            Login
          </button>
        </div>
      </div>
    );
  }

  // ================= MAIN UI =================
  return (
    <div className="flex h-screen bg-gray-900 text-white">

      {/* SIDEBAR */}
      <div className="w-72 bg-black p-4 flex flex-col">

        <button onClick={createSession} className="bg-blue-600 p-2 mb-3">
          + New Chat
        </button>

        <select
          className="text-black p-2 mb-3"
          onChange={(e) => setSelectedDocId(e.target.value)}
        >
          <option>Select Document</option>
          {documents.map((d) => (
            <option key={d.id} value={d.id}>{d.fileName}</option>
          ))}
        </select>

        {/* CHAT SESSIONS */}
        <div className="flex-1 overflow-y-auto">
          {sessions.map((s) => (
            <div key={s.id} className="flex justify-between bg-gray-800 p-2 mb-2">
              <span onClick={() => setSelectedSessionId(s.id)}>
                {s.name || "New Chat"}
              </span>
              <div>
                <button onClick={() => renameSession(s.id)}>✏</button>
                <button onClick={() => deleteSession(s.id)}>❌</button>
              </div>
            </div>
          ))}
        </div>

        <button onClick={logout} className="bg-red-600 p-2 mt-2">
          Logout
        </button>

      </div>

      {/* CHAT */}
      <div className="flex-1 flex flex-col">

        <div className="flex-1 p-4 overflow-y-auto">
          {messages.map((m, i) => (
            <div key={i} className={`p-2 mb-2 ${m.role === "user" ? "text-right" : ""}`}>
              {m.text}
            </div>
          ))}
          {loading && <p>Thinking...</p>}
          <div ref={chatEndRef}></div>
        </div>

        <div className="p-3 flex">
          <textarea
            className="flex-1 p-2 text-black"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={handleKeyDown}
          />
          <button onClick={sendMessage} className="bg-blue-600 px-4">
            Send
          </button>
        </div>

      </div>
    </div>
  );
}