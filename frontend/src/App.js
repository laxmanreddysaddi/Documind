import { useState, useEffect, useRef } from "react";
import axios from "axios";

const BASE_URL =
  process.env.REACT_APP_API_URL ||
  "https://documind-backend-30m4.onrender.com/api";

const api = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

// ✅ Attach token per user
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
  const savedUser = localStorage.getItem("currentUser");

  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState(savedUser || "");
  const [password, setPassword] = useState("");
  const [token, setToken] = useState(
    savedUser ? localStorage.getItem(`token_${savedUser}`) : ""
  );
  const [authLoading, setAuthLoading] = useState(false);

  // ================= DATA =================
  const [documents, setDocuments] = useState([]);
  const [selectedDocId, setSelectedDocId] = useState("");

  const [sessions, setSessions] = useState([]);
  const [selectedSessionId, setSelectedSessionId] = useState("");

  const [messages, setMessages] = useState([]);
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);

  const chatEndRef = useRef(null);

  // ================= BACKEND WAKE-UP =================
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

  // ================= CHAT =================
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

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const logout = () => {
    localStorage.removeItem("currentUser");
    setToken("");
    setMessages([]);
    setDocuments([]);
  };

  // ================= LOGIN UI =================
  if (!token) {
    return (
      <div className="flex h-screen bg-gradient-to-br from-black to-gray-900">

        {/* LEFT SIDE */}
        <div className="w-1/2 flex flex-col items-center justify-center text-white">
          <h1 className="text-5xl font-bold mb-4">DocuMind AI</h1>
          <p className="text-gray-400 text-center max-w-sm">
            Smart document assistant powered by AI.
          </p>
        </div>

        {/* RIGHT SIDE */}
        <div className="w-1/2 flex items-center justify-center">
          <div className="backdrop-blur-xl bg-white/10 p-8 rounded-xl w-80 text-white">

            <h2 className="text-2xl mb-6 text-center">
              {isLogin ? "Login" : "Register"}
            </h2>

            <input
              className="w-full p-2 mb-4 bg-white/20"
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />

            <input
              type="password"
              className="w-full p-2 mb-4 bg-white/20"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />

            <button
              onClick={handleAuth}
              className="w-full bg-blue-600 p-2 rounded"
            >
              {authLoading
                ? "Please wait..."
                : isLogin
                ? "Login"
                : "Register"}
            </button>

            <p
              className="text-sm mt-4 text-center cursor-pointer text-blue-300"
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
    <div className="flex h-screen bg-black text-white">

      <div className="w-64 p-3 bg-white/10">
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
            placeholder="Ask something..."
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