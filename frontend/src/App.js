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
  const currentUser = localStorage.getItem("currentUser");
  const token = currentUser
    ? localStorage.getItem(`token_${currentUser}`)
    : null;

  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default function App() {
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [token, setToken] = useState("");
  const [authLoading, setAuthLoading] = useState(false);

  const [documents, setDocuments] = useState([]);
  const [selectedDocId, setSelectedDocId] = useState("");

  const [sessions, setSessions] = useState([]);
  const [selectedSessionId, setSelectedSessionId] = useState("");

  const [messages, setMessages] = useState([]);
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);

  const [uploading, setUploading] = useState(false);

  const chatEndRef = useRef(null);

  useEffect(() => {
    fetch(`${BASE_URL}/auth/login`, { method: "OPTIONS" }).catch(() => {});
  }, []);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

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

  const handleUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setUploading(true);

    const formData = new FormData();
    formData.append("file", file);
    formData.append("username", username);

    try {
      await api.post("/documents/upload", formData);
      fetchDocuments();
    } catch {
      alert("Upload failed");
    }

    setUploading(false);
  };

  const fetchSessions = async (docId) => {
    if (!docId) return;

    try {
      const res = await api.get("/chat/sessions", {
        params: { username, documentId: docId },
      });
      setSessions(res.data || []);
    } catch {}
  };

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

  const handleAuth = async () => {
    if (!username || !password) return alert("Enter details");

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
        alert("Registered! Login now");
        setIsLogin(true);
      }
    } catch {
      alert("Auth failed");
    }

    setAuthLoading(false);
  };

  const deleteDocument = async (id) => {
    if (!window.confirm("Delete document?")) return;
    await api.delete(`/documents/delete/${id}`);
    fetchDocuments();
  };

  const sendMessage = async () => {
    if (!question.trim()) return;
    if (!selectedDocId) return alert("Select document");

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
  };

  // LOGIN UI
  if (!token) {
    return (
      <div className="flex h-screen bg-black text-white items-center justify-center">

        <h1 className="absolute left-10 text-3xl font-bold">
          DocuMind AI
        </h1>

        <div className="bg-white/10 p-6 rounded w-80">
          <h2 className="text-xl mb-4 text-center">
            {isLogin ? "Login" : "Register"}
          </h2>

          <input
            className="w-full p-2 mb-3 bg-white/20"
            placeholder="Username"
            onChange={(e) => setUsername(e.target.value)}
          />

          <input
            type="password"
            className="w-full p-2 mb-3 bg-white/20"
            placeholder="Password"
            onChange={(e) => setPassword(e.target.value)}
          />

          <button onClick={handleAuth} className="w-full bg-blue-600 p-2">
            {authLoading ? "Please wait..." : isLogin ? "Login" : "Register"}
          </button>

          <p
            className="text-sm mt-3 text-center cursor-pointer text-blue-300"
            onClick={() => setIsLogin(!isLogin)}
          >
            New user, Register
          </p>
        </div>
      </div>
    );
  }

  // MAIN UI
  return (
    <div className="flex h-screen bg-black text-white">

      {/* LEFT */}
      <div className="w-64 p-3 bg-white/10 flex flex-col">

        <h1 className="text-xl font-bold text-center mb-3">
          DocuMind AI
        </h1>

        <label className="bg-gray-700 p-2 text-center cursor-pointer mb-2">
          {uploading ? "Uploading..." : "Choose File"}
          <input type="file" hidden onChange={handleUpload} />
        </label>

        <button
          onClick={() => {
            setSelectedSessionId("");
            setMessages([]);
          }}
          className="bg-blue-600 w-full p-2 mb-3"
        >
          + New Chat
        </button>

        {documents.map((d) => (
          <div
            key={d.id}
            onClick={() => {
              setSelectedDocId(d.id);
              setSelectedSessionId("");
            }}
            className="p-2 cursor-pointer flex justify-between hover:bg-gray-700"
          >
            <span>{d.fileName}</span>
            <button onClick={(e) => {
              e.stopPropagation();
              deleteDocument(d.id);
            }}>🗑</button>
          </div>
        ))}

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

          {messages.map((m, i) => (
            <div
              key={i}
              className={`flex mb-3 ${
                m.role === "user" ? "justify-end" : "justify-start"
              }`}
            >
              <div
                className={`px-4 py-2 rounded-lg max-w-[70%] ${
                  m.role === "user"
                    ? "bg-blue-600 text-white"
                    : "bg-gray-700 text-gray-200"
                }`}
              >
                {m.text}
              </div>
            </div>
          ))}

          {loading && <p className="text-gray-400">Thinking...</p>}

          <div ref={chatEndRef}></div>
        </div>

        <div className="p-3 flex gap-2">
          <input
  className="flex-1 p-2 bg-white/20"
  placeholder="Ask something..."
  value={question}
  onChange={(e) => setQuestion(e.target.value)}

  // 🔥 ENTER TO SEND
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