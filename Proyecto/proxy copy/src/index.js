import express from 'express';
import cors from 'cors';
import { getBoard, setCell, resetBoard, solveBoard, validateGame } from './services/proxyService.js';



const app = express();
const PORT = 3001;

app.use(cors());
app.use(express.json());

app.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept');
    next();
});

app.get('/board', async (req, res) => {
    let respBack = await getBoard();
    if (req.query.id && req.query.id !== '') {
        respBack = { ...respBack, id: req.query.id };
    }
    res.status(200).json(respBack);
});

// app.get('/board/:id', async (req, res) => {
//     const { id } = req.params;
//     const respBack = await getBoard();
//     res.status(200).json({ ...respBack, id });
// });

app.put('/board', async (req, res) => {
    try {
        if (!req.body) {
            return res.status(400).json({ error: 'Request body is missing' });
        }
        if (typeof req.body.row !== 'number') {
            return res.status(400).json({ error: 'Invalid or missing row' });
        }
        if (typeof req.body.column !== 'number') {
            return res.status(400).json({ error: 'Invalid or missing column' });
        }
        if (typeof req.body.value !== 'number') {
            return res.status(400).json({ error: 'Invalid or missing value' });
        }
        const { row, column, value } = req.body;
        const respBack = await setCell(row, column, value);
        res.status(200).json(respBack);
    } catch (error) {
        console.error('Error in PUT /board:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

app.post('/board', async (req, res) => {
    try {
        const respBack = await resetBoard();
        res.status(200).json(respBack);
    } catch (error) {
        console.error('Error in POST /board/reset:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

app.put('/board/solution', async (req, res) => {
    try {
        const respBack = await solveBoard();
        res.status(200).json(respBack);
    } catch (error) {
        console.error('Error in PUT /board/solve:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

app.get('/board/solution', async (req, res) => {
    try {
        const respBack = await validateGame();
        res.status(200).json(respBack);
    } catch (error) {
        console.error('Error in GET /board/validate:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

app.listen(PORT, '0.0.0.0', () => {
    console.log(`Proxy server is running on http://localhost:${PORT}`);
});