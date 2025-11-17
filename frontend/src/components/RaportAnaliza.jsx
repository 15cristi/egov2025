import React, { useState, useEffect } from "react";
import "./FormularPlata.css";
import { saveAs } from "file-saver";

const REPORT_URL = "/api/raport/analiza";

const RaportAnaliza = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [pdfUrl, setPdfUrl] = useState(null);


  const fetchReport = async () => {
    setLoading(true);
    setError(null);
    setPdfUrl(null);

    try {
      const response = await fetch(REPORT_URL);
      if (!response.ok) {
        throw new Error(`Eroare HTTP: ${response.status}`);
      }

      
      const pdfBlob = await response.blob();

     
      const url = URL.createObjectURL(pdfBlob);
      setPdfUrl(url);
      
    } catch (err) {
      console.error("Eroare la preluarea raportului:", err);
      setError("❌ Nu s-a putut genera sau prelua raportul de analiză.");
    } finally {
      setLoading(false);
    }
  };


  const handleDownload = () => {
    if (pdfUrl) {
       
        saveAs(pdfUrl, "raport_analiza_egov.pdf");
    } else {
     
        fetch(REPORT_URL)
            .then(res => res.blob())
            .then(blob => saveAs(blob, "raport_analiza_egov.pdf"))
            .catch(err => {
                console.error(err);
                setError("Eroare la descărcarea fișierului.");
            });
    }
  }

  
  useEffect(() => {
    fetchReport();
    
   
    return () => {
        if (pdfUrl) {
            URL.revokeObjectURL(pdfUrl);
        }
    }
  }, []);

  return (
    <div className="form-wrapper">
      <div className="form-card" style={{ maxWidth: '800px', padding: '20px' }}>
        <h2>Raport de Analiză Statistică 📊</h2>
        
        {loading && <p className="total-info">Se generează raportul...</p>}
        {error && <p className="error-text">{error}</p>}

        {!loading && pdfUrl && (
          <div style={{ textAlign: 'center' }}>
            
            <button 
              className="btn-submit"
              onClick={handleDownload}
              style={{ marginBottom: '15px' }}
            >
              ⬇️ Descarcă Raportul PDF
            </button>
            
            <p className="total-info" style={{ marginBottom: '10px' }}>Previzualizare:</p>
            
            {}
            <iframe
              title="Raport Analiza"
              src={pdfUrl}
              style={{ width: '100%', height: '70vh', border: '1px solid var(--gold-primary)' }}
              frameBorder="0"
            >
              Acest browser nu suportă vizualizarea PDF-urilor încorporate.
            </iframe>

          </div>
        )}
        
        <p className="form-footer" style={{ marginTop: '20px' }}>
            Raport generat de serverul Java Spring Boot utilizând iText.
        </p>
      </div>
    </div>
  );
};

export default RaportAnaliza;