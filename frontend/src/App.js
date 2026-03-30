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
  const chatEndRef = useRef(null);

  // ================= AUTH FUNCTION =================
  const handleAuth = async () => {
    if (!username || !password) {
      alert("Enter username & password");
      return;
    }

    try {
      const endpoint = isLogin ? "/auth/login" : "/auth/register";

      const res = await api.post(endpoint, {
        username,
        password,
      });

      if (isLogin) {
        const token = res.data.token;
        localStorage.setItem("token", token);
        localStorage.setItem("username", username);
        setToken(token);
      } else {
        alert("Registered successfully");
        setIsLogin(true);
      }

    } catch (err) {
      alert("Auth failed");
    }
  };

  // ================= AUTO SCROLL =================
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // ================= FETCH DOCS =================
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

  // ================= FETCH CHAT =================
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

  // ================= CREATE SESSION =================
  const createSession = async () => {
    const res = await api.post("/chat/session/create", null, {
      params: { username, documentId: selectedDocId },
    });

    fetchSessions(selectedDocId);
    setSelectedSessionId(res.data.id);
  };

  // ================= DELETE SESSION =================
  const deleteSession = async (id) => {
    await api.delete(`/chat/session/delete/${id}`);
    fetchSessions(selectedDocId);

    if (selectedSessionId === id) {
      setMessages([]);
      setSelectedSessionId("");
    }
  };

  // ================= SEND =================
  const sendMessage = async () => {
    if (!question.trim()) return;

    if (!selectedSessionId) {
      alert("Select chat first");
      return;
    }

    const q = question;
    setQuestion("");

    setMessages((prev) => [...prev, { role: "user", text: q }]);
    setLoading(true);

    try {
      const res = await api.post("/chat/ask", null, {
        params: { question: q, documentId: selectedDocId, sessionId: selectedSessionId },
      });

      setMessages((prev) => [...prev, { role: "ai", text: res.data }]);

    } catch {
      setMessages((prev) => [...prev, { role: "ai", text: "Error" }]);
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
    localStorage.clear();
    setToken("");
    setMessages([]);
  };

  // ================= LOGIN UI =================
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
            {isLogin ? "Login" : "Register"}
          </button>

          <p
            className="text-blue-500 mt-2 cursor-pointer"
            onClick={() => setIsLogin(!isLogin)}
          >
            {isLogin ? "Create account" : "Already have account?"}
          </p>
        </div>
      </div>
    );
  }

  // ================= MAIN UI =================
  return (
    <div className="flex h-screen bg-gray-900 text-white">

      {/* SIDEBAR */}
      <div className="w-72 bg-black p-4 flex flex-col">

        <select
          className="text-black p-2 mb-3"
          onChange={(e) => setSelectedDocId(e.target.value)}
        >
          <option>Select Document</option>
          {documents.map((d) => (
            <option key={d.id} value={d.id}>{d.fileName}</option>
          ))}
        </select>

        <button onClick={createSession} className="bg-blue-600 p-2 mb-3">
          + New Chat
        </button>

        <div className="flex-1 overflow-y-auto">
          {sessions.map((s) => (
            <div key={s.id} className="flex justify-between bg-gray-800 p-2 mb-2">
              <span onClick={() => setSelectedSessionId(s.id)}>
                {s.name || "New Chat"}
              </span>
              <button onClick={() => deleteSession(s.id)}>❌</button>
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