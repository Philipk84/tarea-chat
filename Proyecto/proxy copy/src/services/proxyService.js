import net from 'net';

const PORT = 12345;
const HOST = 'localhost';

async function getBoard() {
    const socket = new net.Socket();
    const promi = new Promise((resolve, reject) => {
        socket.connect(PORT, HOST, () => {
            console.log('Connected to TCP server by getBoard');
            const request = {
                action: 'GET_BOARD',
                data: {}
            };
            socket.write(JSON.stringify(request));
            socket.write('\n');
            socket.on('data', (data) => {
                try {
                    const response = JSON.parse(data.toString().trim());
                    resolve(response);
                } catch (error) {
                    reject(error);
                } finally {
                    socket.destroy();
                }
            });
        });
    });
    return promi;
}

async function setCell(row, col, value) {
    const socket = new net.Socket();
    const promi = new Promise((resolve, reject) => {
        socket.connect(PORT, HOST, () => {
            console.log('Connected to TCP server by setCell');
            const request = {
                action: 'SET_CELL',
                data: { i: row, j: col, value }
            };
            socket.write(JSON.stringify(request));
            socket.write('\n');
            socket.on('data', (data) => {
                try {
                    const response = JSON.parse(data.toString().trim());
                    resolve(response);
                } catch (error) {
                    reject(error);
                } finally {
                    socket.destroy();
                }
            });
        });
    });
    return promi;
}

async function resetBoard() {
    const socket = new net.Socket();
    const promi = new Promise((resolve, reject) => {
        socket.connect(PORT, HOST, () => {
            console.log('Connected to TCP server by resetBoard');
            const request = {
                action: 'INIT_GAME',
                data: {}
            };
            socket.write(JSON.stringify(request));
            socket.write('\n');
            socket.on('data', (data) => {
                try {
                    const response = JSON.parse(data.toString().trim());
                    resolve(response);
                } catch (error) {
                    reject(error);
                } finally {
                    socket.destroy();
                }
            });
        });
    });
    return promi;
}

async function validateGame() {
    const socket = new net.Socket();
    const promi = new Promise((resolve, reject) => {
        socket.connect(PORT, HOST, () => {
            console.log('Connected to TCP server by validateGame');
            const request = {
                action: 'VALIDATE_GAME',
                data: {}
            };
            socket.write(JSON.stringify(request));
            socket.write('\n');
            socket.on('data', (data) => {
                try {
                    const response = JSON.parse(data.toString().trim());
                    resolve(response);
                } catch (error) {
                    reject(error);
                } finally {
                    socket.destroy();
                }
            });
        });
    });
    return promi;
}

async function solveBoard() {
    const socket = new net.Socket();
    const promi = new Promise((resolve, reject) => {
        socket.connect(PORT, HOST, () => {
            console.log('Connected to TCP server by solveBoard');
            const request = {
                action: 'SURRENDER',
                data: {}
            };
            socket.write(JSON.stringify(request));
            socket.write('\n');
            socket.on('data', (data) => {
                try {
                    const response = JSON.parse(data.toString().trim());
                    resolve(response);
                } catch (error) {
                    reject(error);
                } finally {
                    socket.destroy();
                }
            });
        });
    });
    return promi;
}

export { getBoard, setCell, resetBoard, solveBoard, validateGame };