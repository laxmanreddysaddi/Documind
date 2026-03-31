import { useState, useEffect, useRef } from "react";
import axios from "axios";

const BASE_URL =
  process.env.REACT_APP_API_URL ||
  "https://documind-backend-30m4.onrender.com/api";

const api = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

// 🔥 USER BASED TOKEN
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
  const currentUser = localStorage.getItem("currentUser");

  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState(currentUser || "");
  const [password, setPassword] = useState("");

  const [token, setToken] = useState(
    currentUser ? localStorage.getItem(`token_${currentUser}`) : ""
  );

  // ================= DATA =================
  const [documents, setDocuments] = useState([]);
  const [selectedDocId, setSelectedDocId] = useState("");

  const [sessions, setSessions] = useState([]);
  const [selectedSessionId, setSelectedSessionId] = useState("");

  const [messages, setMessages] = useState([]);
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);

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
    if (!window.confirm("Delete document?")) return;

    await api.delete(`/documents/delete/${id}`);
    fetchDocuments();
  };

  // ================= AUTH =================
  const handleAuth = async () => {
    if (!username || !password) {
      alert("Enter username & password");
      return;
    }

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
  };

  // ================= CHAT =================
  const sendMessage = async () => {
    if (!question.trim()) return;

    if (!selectedDocId) {
      alert("Select document first");
      return;
    }

    const q = question;
    setQuestion("");

    // ✅ show immediately
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
        { role: "ai", text: "Error getting answer" },
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

  // ================= LOGOUT =================
  const logout = () => {
    localStorage.removeItem("currentUser");
    setToken("");
    setMessages([]);
    setDocuments([]);
  };

  // ================= LOGIN UI =================
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
            {isLogin ? "Login" : "Register"}
          </button>

          <p
            className="text-sm mt-3 text-center cursor-pointer text-blue-300"
            onClick={() => setIsLogin(!isLogin)}
          >
            {isLogin
              ? "New user? Register"
              : "Already have account? Login"}
          </p>

        </div>
      </div>
    );
  }

  // ================= MAIN UI =================
  return (
    <div className="flex h-screen bg-black text-white">

      {/* HEADER */}
      <div className="absolute top-0 w-full text-center p-3 font-bold text-xl bg-white/10">
        DocuMind AI
      </div>

      {/* SIDEBAR */}
      <div className="w-64 p-3 mt-12 bg-white/10">

        <button onClick={createSession}
          className="bg-blue-600 w-full p-2 mb-2">
          + New Chat
        </button>

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
      <div className="flex-1 flex flex-col mt-12">

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
            placeholder="Ask something..."   // ✅ ADDED
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