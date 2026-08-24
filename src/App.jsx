import logo from './assets/logo.png'
import './App.css'

function App() {
  
  return (
    <div className="app-container">
      {/* Sección Superior: Logo y Bienvenida */}
      <header className="hero-section">
        <div className="logo-container">
          <div className="logo-icon">
            <img src={logo} alt="Logo" className="logo-image" />
          </div>
          <h1 className="brand-name">
            STOCK<span className="highlight">FLOW</span>
          </h1>
        </div>
        <p className="tagline">Control total, flujo constante.</p>
        <p className="sub-tagline">
          Gestión inteligente FEFO para tu comercio.
        </p>
      </header>

      {/* Sección Central: Botones de Acción */}
      <main className="action-section">
        <div className="card-info">
          <h3>¡Bienvenido!</h3>
          <p>Selecciona una opción para comenzar a operar.</p>
        </div>

        <button className="btn btn-primary">
          <span className="icon">👤</span> Iniciar Sesión
        </button>
        
        <button className="btn btn-secondary">
          <span className="icon">📷</span> Escáner Rápido
        </button>
      </main>

      {/* Sección Inferior: Footer */}
      <footer className="footer">
        <p>V 1.0.0 | Modo Offline Preparado 🟢</p>
      </footer>
    </div>
  );
}

export default App
