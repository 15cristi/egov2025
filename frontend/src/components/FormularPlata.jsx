import React, { useState, useEffect  } from "react";
import { saveAs } from "file-saver";
import { trimitePlata } from "../api/plataApi";
import { verificaCupon } from "../api/cuponApi";
import "./FormularPlata.css";


const FormularPlata = () => {
  const [form, setForm] = useState({
    email: "",
    telefon: "",
    inmatriculare: "",
    durata: 1,
  });

  const [error, setError] = useState("");
  const [total, setTotal] = useState(null);
  const [costuri, setCosturi] = useState({ suma: null, tva: null, total: null });
  const [reducere, setReducere] = useState(0);
  const [cod, setCod] = useState("");
  const [mesajReducere, setMesajReducere] = useState("");

  const preturi = { 1: 5, 2: 10, 5: 20, 24: 50 };

 const calculeazaCosturi = (durata, reducere = 0) => {
  const suma = preturi[durata];
  const tva = (suma * 0.19).toFixed(2);
  const totalInitial = suma + parseFloat(tva);
  const totalRedus = totalInitial - (totalInitial * reducere) / 100;
  return { suma: suma.toFixed(2), tva, total: totalRedus.toFixed(2) };
};



  const handleChange = (e) => {
  const { name, value } = e.target;
  setForm({ ...form, [name]: value });
  if (name === "durata") {
  const rezultat = calculeazaCosturi(Number(value), reducere);
  setCosturi(rezultat);
}


};

const handleAplicareReducere = () => {
  aplicaReducere();
  const rezultat = calculeazaCosturi(Number(form.durata), reducere);
  setCosturi(rezultat);
};


 const aplicaReducere = async () => {
  const codCurat = cod.trim().toUpperCase();
  if (!codCurat) {
    setMesajReducere("Introduceți un cod de reducere.");
    return;
  }

  try {
    const raspuns = await verificaCupon(codCurat);

    if (raspuns.valid) {
      setReducere(raspuns.procent);
      setMesajReducere(raspuns.mesaj);
    } else {
      setReducere(0);
      setMesajReducere(raspuns.mesaj);
    }

    
    if (form.durata) {
      const rezultat = calculeazaCosturi(Number(form.durata), raspuns.procent);
      setCosturi(rezultat);
    }
  } catch (err) {
    console.error(err);
    setReducere(0);
    setMesajReducere("Eroare la verificarea codului.");
  }
};



useEffect(() => {
  if (form.durata) {
    const rezultat = calculeazaCosturi(Number(form.durata), reducere);
    setCosturi(rezultat);
  }
}, [reducere, form.durata]);




 const handleSubmit = async (e) => {
  e.preventDefault();

  if (!form.email.includes("@")) {
    setError("Emailul nu este valid!");
    return;
  }

  if (!costuri.total) {
    setError("Selectați durata pentru a calcula totalul!");
    return;
  }

  try {
   
    const payload = {
      ...form,
      suma: parseFloat(costuri.suma),
      tva: parseFloat(costuri.tva),
      total: parseFloat(costuri.total),
      reducere: reducere,
      codReducere: cod.trim().toUpperCase(), 
    };

    
    const pdfBlob = await trimitePlata(payload);

    
    saveAs(pdfBlob, "ordin_plata.pdf");

    alert("✅ Plata a fost înregistrată cu succes!");
    setError("");
  } catch (err) {
    console.error(err);
    setError("❌ Eroare la trimiterea datelor către server.");
  }
};



  return (
    <div className="form-wrapper">
  <form onSubmit={handleSubmit} className="form-card">
    <h2>Formular Plata Parcare</h2>

    <input type="email" name="email" placeholder="Adresa de email"
           className="input-field" onChange={handleChange} required />

    <input type="text" name="telefon" placeholder="Număr de telefon"
           className="input-field" onChange={handleChange} required />

    <input type="text" name="inmatriculare" placeholder="Număr de înmatriculare"
           className="input-field" onChange={handleChange} required />

    <select name="durata" className="input-field select-field" onChange={handleChange}>
      <option value="1">1h - 5 RON</option>
      <option value="2">2h - 10 RON</option>
      <option value="5">5h - 20 RON</option>
      <option value="24">24h - 50 RON</option>
    </select>

    {costuri.total && (
  <div className="total-section">
    <p><b>Cost fără TVA:</b> {costuri.suma} RON</p>
    <p><b>TVA (19%):</b> {costuri.tva} RON</p>
    <hr style={{ border: "0.5px solid rgba(217,164,65,0.3)", margin: "6px 0" }} />
    <p><b>Total plată:</b> {costuri.total} RON</p>
  </div>
)}
  <div className="reducere-row">
  <input
    type="text"
    name="cod"
    value={cod}
    onChange={(e) => setCod(e.target.value)}
    placeholder="Cod reducere"
    className="input-reducere"
  />
  <button
  type="button"
  onClick={aplicaReducere}
  className="btn-aplica"
>
  Aplică
</button>
{mesajReducere && (
  <p className="mesaj-reducere">{mesajReducere}</p>
)}

</div>
<br></br>


    {error && <p className="error-text">{error}</p>}

    <button type="submit" className="btn-submit">Trimite și descarcă PDF</button>
    <p className="form-footer">Datele sunt protejate conform GDPR</p>
  </form>
</div>
  );
};

export default FormularPlata;
