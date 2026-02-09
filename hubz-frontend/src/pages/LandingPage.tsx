import { ArrowRight, Zap, Users, Target, Calendar, BarChart3, Shield } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const features = [
  {
    icon: Users,
    title: 'Organisations',
    description: 'Gerez vos equipes et membres facilement.',
  },
  {
    icon: Zap,
    title: 'Taches',
    description: 'Kanban, assignation et suivi en temps reel.',
  },
  {
    icon: Target,
    title: 'Objectifs',
    description: 'Definissez et suivez vos objectifs personnels et collectifs.',
  },
  {
    icon: Calendar,
    title: 'Calendrier',
    description: 'Planifiez vos evenements et deadlines.',
  },
  {
    icon: BarChart3,
    title: 'Analytics',
    description: 'Visualisez votre productivite avec des tableaux de bord.',
  },
  {
    icon: Shield,
    title: 'Securise',
    description: 'Vos donnees sont protegees avec un chiffrement moderne.',
  },
];

export default function LandingPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-[#0A0A0F] text-white">
      {/* Nav */}
      <nav className="border-b border-white/10 px-6 py-4">
        <div className="max-w-6xl mx-auto flex items-center justify-between">
          <span className="text-2xl font-bold tracking-tight">
            <span className="text-accent">H</span>ubz
          </span>
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/login')}
              className="px-4 py-2 text-sm text-gray-300 hover:text-white transition-colors"
            >
              Connexion
            </button>
            <button
              onClick={() => navigate('/register')}
              className="px-4 py-2 text-sm bg-accent hover:bg-blue-600 rounded-lg transition-colors"
            >
              S'inscrire
            </button>
          </div>
        </div>
      </nav>

      {/* Hero */}
      <section className="px-6 pt-24 pb-20">
        <div className="max-w-4xl mx-auto text-center">
          <h1 className="text-5xl md:text-6xl font-bold leading-tight mb-6">
            Toute votre productivite
            <br />
            <span className="text-accent">en un seul endroit</span>
          </h1>
          <p className="text-lg text-gray-400 max-w-2xl mx-auto mb-10">
            Hubz centralise vos organisations, equipes, taches, objectifs et habitudes dans une interface simple et moderne.
          </p>
          <div className="flex items-center justify-center gap-4">
            <button
              onClick={() => navigate('/register')}
              className="inline-flex items-center gap-2 px-6 py-3 bg-accent hover:bg-blue-600 rounded-lg text-base font-medium transition-colors"
            >
              Commencer gratuitement
              <ArrowRight className="w-4 h-4" />
            </button>
            <button
              onClick={() => navigate('/login')}
              className="px-6 py-3 border border-white/20 hover:border-white/40 rounded-lg text-base transition-colors"
            >
              Se connecter
            </button>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="px-6 py-20 bg-white/[0.02]">
        <div className="max-w-6xl mx-auto">
          <h2 className="text-3xl font-bold text-center mb-12">
            Tout ce dont vous avez besoin
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {features.map((feature) => (
              <div
                key={feature.title}
                className="p-6 rounded-xl bg-white/5 border border-white/10 hover:border-accent/30 transition-colors"
              >
                <feature.icon className="w-8 h-8 text-accent mb-4" />
                <h3 className="text-lg font-semibold mb-2">{feature.title}</h3>
                <p className="text-sm text-gray-400">{feature.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="px-6 py-20">
        <div className="max-w-3xl mx-auto text-center">
          <h2 className="text-3xl font-bold mb-4">
            Pret a commencer ?
          </h2>
          <p className="text-gray-400 mb-8">
            Creez votre compte gratuitement et commencez a organiser votre travail.
          </p>
          <button
            onClick={() => navigate('/register')}
            className="inline-flex items-center gap-2 px-8 py-3 bg-accent hover:bg-blue-600 rounded-lg text-base font-medium transition-colors"
          >
            Creer mon compte
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-white/10 px-6 py-8">
        <div className="max-w-6xl mx-auto flex items-center justify-between text-sm text-gray-500">
          <span>Hubz {new Date().getFullYear()}</span>
          <div className="flex items-center gap-6">
            <a href="#" className="hover:text-gray-300 transition-colors">Contact</a>
            <a href="#" className="hover:text-gray-300 transition-colors">Conditions</a>
          </div>
        </div>
      </footer>
    </div>
  );
}
