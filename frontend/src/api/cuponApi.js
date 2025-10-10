import axios from "axios";

const API_BASE = "http://localhost:8080/api";

export const verificaCupon = async (cod) => {
  try {
    const response = await axios.get(`${API_BASE}/cupon/${cod}`);
    // răspunsul este un string de tipul: "✅ Cupon valid - reducere 25%"
    return response.data;
  } catch (err) {
    console.error("Eroare la verificarea cuponului da da :", err);
    throw err;
  }
};


