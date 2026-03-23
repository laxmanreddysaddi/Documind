import React,{useState} from "react";
import api from "../api/api";

export default function ChatPanel(){

    const [question,setQuestion] = useState("");
    const [messages,setMessages] = useState([]);

    const ask = async () =>{

        const userMessage={
            role:"user",
            text:question
        };

        setMessages(prev=>[...prev,userMessage]);

        try{

            const res = await api.get("/chat",{
                params:{question}
            });

            const aiMessage={
                role:"ai",
                text:res.data
            };

            setMessages(prev=>[...prev,aiMessage]);

        }catch(err){

            alert("Chat failed");

        }

        setQuestion("");

    };

    return(

        <div>

            <h3>DocuMind AI</h3>

            <div style={{
                height:"400px",
                border:"1px solid gray",
                overflowY:"auto",
                padding:"10px"
            }}>

                {messages.map((msg,i)=>(

                    <div key={i} style={{marginBottom:"10px"}}>

                        {msg.role==="user" ?
                            <p><b>You:</b> {msg.text}</p>
                            :
                            <p><b>AI:</b> {msg.text}</p>
                        }

                    </div>

                ))}

            </div>

            <input
                value={question}
                onChange={(e)=>setQuestion(e.target.value)}
                placeholder="Ask about your document..."
            />

            <button onClick={ask}>
                Send
            </button>

        </div>

    );

}