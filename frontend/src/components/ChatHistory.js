import React,{useEffect,useState} from "react";
import api from "../api/api";

export default function ChatHistory(){

    const [history,setHistory] = useState([]);

    useEffect(()=>{
        loadHistory();
    },[]);

    const loadHistory = async () =>{
        const res = await api.get("/chat/history");
        setHistory(res.data);
    }

    const deleteChat = async(id)=>{
        await api.delete("/chat/history/"+id);
        loadHistory();
    }

    return(
        <div>

            <h3>Chat History</h3>

            {history.map(chat =>(

                <div key={chat.id}
                     style={{border:"1px solid gray",margin:"10px",padding:"10px"}}>

                    <p><b>Question:</b> {chat.question}</p>
                    <p><b>Answer:</b> {chat.answer}</p>

                    <button onClick={()=>deleteChat(chat.id)}>
                        Delete
                    </button>

                </div>

            ))}

        </div>
    );
}