import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api";

// trimite datele formularului către backend
export const trimitePlata = async (formData) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/submit`, formData, {
      responseType: "blob", // pentru a descărca PDF-ul
    });
    return response.data;
  } catch (error) {
    console.error("Eroare la trimiterea formularului:", error);
    throw error;
  }
};

// (opțional) obține lista tuturor plăților din backend
export const getPlati = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/plati`);
    return response.data;
  } catch (error) {
    console.error("Eroare la obținerea plăților:", error);
    throw error;
  }
};
