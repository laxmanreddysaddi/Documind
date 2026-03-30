import { useState, useEffect, useRef } from "react";
import axios from "axios";


const BASE_URL =
  process.env.REACT_APP_API_URL ||
  "https://documind-backend-30m4.onrender.com/api";

const api = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

export default function App() {

  // ================= AUTH =================
  const [username, setUsername] = useState(localStorage.getItem("username") || "");
  const [password, setPassword] = useState("");
  const [token, setToken] = useState(localStorage.getItem("token") || "");
  const [isLogin, setIsLogin] = useState(true);

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

  // ================= FETCH =================
  const fetchDocuments = async () => {
    try {
      const res = await api.get("/documents/history", {
        params: { username },
      });
      setDocuments(res.data || []);
    } catch {}
  };

  const fetchSessions = async (docId) => {
    try {
      const res = await api.get("/chat/sessions", {
        params: { username, documentId: docId },
      });
      setSessions(res.data || []);
    } catch {}
  };

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
    if (token) fetchDocuments();
  }, [token]);

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

    let sessionId = selectedSessionId;

    if (!sessionId) {
      const res = await api.post("/chat/session/create", null, {
        params: { username, documentId: selectedDocId },
      });
      sessionId = res.data.id;
      setSelectedSessionId(sessionId);
      fetchSessions(selectedDocId);
    }

    const q = question;
    setQuestion("");

    setMessages((prev) => [...prev, { role: "user", text: q }]);
    setLoading(true);

    try {
      const res = await api.post("/chat/ask", null, {
        params: { question: q, documentId: selectedDocId, sessionId },
      });

      const fullText = res.data;

      let index = 0;
      let current = "";

      setMessages((prev) => [...prev, { role: "ai", text: "" }]);

      const interval = setInterval(() => {
        if (index < fullText.length) {
          current += fullText[index];
          index++;

          setMessages((prev) => {
            const updated = [...prev];
            updated[updated.length - 1].text = current;
            return updated;
          });
        } else {
          clearInterval(interval);
          setLoading(false);
        }
      }, 10);

    } catch {
      setMessages((prev) => [
        ...prev,
        { role: "ai", text: "Error" },
      ]);
      setLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  // ================= VOICE =================
  const startVoice = () => {
    if (!("webkitSpeechRecognition" in window)) {
      alert("Voice not supported");
      return;
    }

    const recognition = new window.webkitSpeechRecognition();
    recognition.lang = "en-US";

    recognition.onresult = (event) => {
      setQuestion(event.results[0][0].transcript);
    };

    recognition.start();
  };
  // ================= DELETE DOC =================
  const deleteDocument = async (id) => {
    await api.delete(`/documents/delete/${id}`);

    if (selectedDocId == id) {
      setSelectedDocId("");
      setMessages([]);
      setSessions([]);
    }

    fetchDocuments();
  };

  // ================= LOGIN =================
  if (!token) {
    return (
      <div className="h-screen flex items-center justify-center bg-gradient-to-br from-blue-900 to-black">
        <div className="backdrop-blur-xl bg-white/10 p-8 rounded-2xl w-80 text-white">
          <h2 className="text-2xl font-bold mb-6 text-center">
            {isLogin ? "Login" : "Register"}
          </h2>

          <input
            className="w-full p-2 mb-3 rounded bg-white/20"
            placeholder="Username"
            onChange={(e) => setUsername(e.target.value)}
          />

          <input
            type="password"
            className="w-full p-2 mb-4 rounded bg-white/20"
            placeholder="Password"
            onChange={(e) => setPassword(e.target.value)}
          />

          <button onClick={handleAuth} className="w-full bg-blue-500 p-2 rounded">
            {isLogin ? "Login" : "Register"}
          </button>

          <p className="mt-4 text-center cursor-pointer" onClick={() => setIsLogin(!isLogin)}>
            Switch
          </p>
        </div>
      </div>
    );
  }

  // ================= MAIN =================
  return (
    <div className="flex h-screen bg-gradient-to-br from-black to-gray-900 text-white">

      {/* SIDEBAR */}
      <div className="w-64 p-4 backdrop-blur-xl bg-white/10">

        <button
          onClick={() => setSelectedSessionId("")}
          className="w-full mb-3 p-2 bg-blue-500 rounded"
        >
          + New Chat
        </button>

        <input type="file" className="mb-3" />

        {documents.map((d) => (
          <div key={d.id} className="flex justify-between p-2">
            <span onClick={() => setSelectedDocId(d.id)}>{d.fileName}</span>
            <button onClick={() => deleteDocument(d.id)}>🗑</button>
          </div>
        ))}

        <div className="mt-4">
          {sessions.map((s) => (
            <div key={s.id} onClick={() => setSelectedSessionId(s.id)}>
              {s.name}
            </div>
          ))}
        </div>
      </div>

      {/* CHAT */}
      <div className="flex-1 flex flex-col">

        <div className="flex-1 overflow-y-auto p-6 space-y-4">
          {messages.map((m, i) => (
            <div key={i} className={m.role === "user" ? "text-right" : ""}>
              {m.text}
            </div>
          ))}
          <div ref={chatEndRef}></div>
        </div>

        <div className="p-4 flex gap-2">

          <textarea
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={handleKeyDown}
            className="flex-1 p-3 rounded bg-white/20"
          />

          <button onClick={sendMessage} className="bg-blue-500 px-4">Send</button>
          <button onClick={startVoice} className="bg-purple-500 px-4">🎤</button>
        </div>

        {/* Suggestions */}
        <div className="p-2 flex gap-2 flex-wrap">
          {["Summarize", "Key points", "Explain"].map((s) => (
            <button key={s} onClick={() => setQuestion(s)}>{s}</button>
          ))}
        </div>

      </div>
    </div>
  );
}