import React,{useEffect,useState} from "react";
import api from "../api/api";

export default function DocumentHistory(){

    const [docs,setDocs] = useState([]);

    useEffect(()=>{
        loadDocs();
    },[]);

    const loadDocs = async ()=>{
        const res = await api.get("/documents/history");
        setDocs(res.data);
    }

    const deleteDoc = async(id)=>{
        await api.delete("/documents/"+id);
        loadDocs();
    }

    return(

        <div>

            <h3>Your Documents</h3>

            {docs.map(doc=>(
                <div key={doc.id}
                     style={{border:"1px solid gray",margin:"10px",padding:"10px"}}>

                    <p>{doc.fileName}</p>

                    <button onClick={()=>deleteDoc(doc.id)}>
                        Delete
                    </button>

                </div>
            ))}

        </div>

    );
}