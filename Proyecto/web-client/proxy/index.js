const express = require('express')
const net = require('net')
const cors = require('cors')
const WebSocket = require('ws')


const app = express();
app.use(cors());
app.use(express.json());


const WSPORT = 3002;
const wss = new WebSocket.Server({ port: WSPORT });

wss.on('connection', (ws) =>{
    console.log("Cliente web iniciado")

    ws.on('close', () =>{
        console.log("Cliente web cerrado");

    });

});
const socket = new net.Socket();
let connected = false;

socket.connect(5000, "localhost", () =>{
  connected = true;
  console.log(connected)

    socket.on("data", (data) => {
        const messageStr = data.toString().trim();
        try {
            const message = JSON.parse(messageStr);

            switch(message.command){
                case "GET_MESSAGE":

                        wss.clients.forEach((client) => {
                            if (client.readyState === WebSocket.OPEN) {
                                client.send(messageStr);
                            }
                        });

                    break;

                case "GET_MSG_GROUP":

                    wss.clients.forEach((client) => {
                        if (client.readyState === WebSocket.OPEN) {
                            client.send(messageStr);
                        }
                    });
                    break;

            }

        }catch (e){

        }


    })
});




app.post('/chat',(req,res) =>{
    const { sender, receiver, message } = req.body;
    const backReq = {
        command: "MSG_USER",
        data: {
            "sender" : sender,
            "receiver" : receiver,
            "message" : message,
        }
    }
    const bodyStr = JSON.stringify(backReq)

    if(connected){
        socket.write(bodyStr)
        socket.write("\n")
        socket.once("data", (data) => {
            const message = data.toString().trim();
            try{
                res.json(JSON.parse(message));
            }catch(e){
                res.status(500).json({ error: "Error al procesar la respuesta del servidor" });
            }
        });
    }else{
        res.status(500).json({ error: "Socket no conectado" });
    }


});

app.post('/register', async (req, res) => {
    const { username, clientIp } = req.body;

    const raw = {
        command: "REGISTER",
        data: {
            username: username,
            clientIp: clientIp
        }
    };
    const request = JSON.stringify(raw);



    if (connected) {

        socket.write(request);
        socket.write("\n");
        socket.once("data", (data) => {
            const message = data.toString().trim();

            if (message.includes("OK")) {
                res.json( message );
            } else {
                res.status(409).json(message );
            }
        });

        socket.once('error', (err) => {
            console.error('Error de socket durante el registro:', err);
            res.status(500).json({ error: "Error de conexión con el servidor de chat." });
        });

    } else {
        res.status(503).json({ error: "Socket no conectado al servidor Java." });
    }
});



app.post('/group/create', (req, res) =>{
    const { groupName } = req.body

    const raw = {
        command: "CREATE_GROUP",
        data: {
            group: groupName,
        }
    };
    const request = JSON.stringify(raw);

    if (connected) {

        socket.write(request);
        socket.write("\n");
        socket.once("data", (data) => {
            const message = data.toString().trim();

            if (message.includes("OK")) {
                res.json( message );
            } else {
                res.status(409).json(message );
            }
        });

        socket.once('error', (err) => {
            console.error('Error de socket durante el registro:', err);
            res.status(500).json({ error: "Error de conexión con el servidor de chat." });
        });

    } else {
        res.status(503).json({ error: "Socket no conectado al servidor Java." });
    }
});


app.post('/group/add', (req, res) => {
    const { groupName, members } = req.body;

    const payload = {
        command: "ADD_TO_GROUP",
        data: {
            group : groupName,
            members: members
        }
    };

    if (connected) {
        socket.write(JSON.stringify(payload));
        socket.write("\n");

        socket.once("message", (data) => {
            res.json({ success: true, response: data.toString() });
        });

        socket.once('error', (err) => {
            console.error('Error de socket durante ADD_TO_GROUP:', err);
            res.status(500).json({ error: "Error de conexión con el servidor de chat." });
        });

    } else {
        res.status(503).json({ error: "Socket no conectado al servidor Java." });
    }
});



app.post('/group/message', (req, res) => {
    const { groupName, sender, message } = req.body;

    const payload = {
        command: "MSG_GROUP",
        data: {
            group: groupName,
            sender: sender,
            message: message,
        }
    };

    if (connected) {
        socket.write(JSON.stringify(payload));
        socket.write("\n");

        socket.once("message", (data) => {
            res.json({ success: true, response: data.toString() });
        });

        socket.once('error', (err) => {
            console.error('Error de socket durante MSG_GROUP:', err);
            res.status(500).json({ error: "Error de conexión con el servidor de chat." });
        });

    } else {
        res.status(503).json({ error: "Socket no conectado al servidor Java." });
    }
});


const PORT = 3001;
app.listen(PORT, () => {
    console.log(`Proxy escuchando en http://localhost:${PORT}`);
});