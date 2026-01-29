import { ArrowRight, Check, Zap, Users, Target, Calendar, TrendingUp, Shield, Sparkles, X, Crown, Building2, User, CheckCircle2, Star } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import Button from '../components/ui/Button';
import Card from '../components/ui/Card';

export default function LandingPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0A0A0F] via-[#1A1A24] to-[#0A0A0F]">
      {/* Navigation */}
      <nav className="fixed top-0 left-0 right-0 z-50 backdrop-blur-lg bg-[#0A0A0F]/80 border-b border-white/10">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-accent to-purple-600 flex items-center justify-center">
              <Sparkles className="w-6 h-6 text-white" />
            </div>
            <span className="text-2xl font-bold text-white">Hubz</span>
          </div>
          <div className="flex items-center gap-4">
            <Button variant="ghost" onClick={() => navigate('/login')}>
              Connexion
            </Button>
            <Button onClick={() => navigate('/register')}>
              Commencer gratuitement
              <ArrowRight className="w-4 h-4" />
            </Button>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="pt-32 pb-20 px-6">
        <div className="max-w-7xl mx-auto text-center">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-accent/10 border border-accent/20 text-accent mb-6">
            <Sparkles className="w-4 h-4" />
            <span className="text-sm font-medium">La productivité réinventée</span>
          </div>

          <h1 className="text-6xl md:text-7xl font-bold text-white mb-6 leading-tight">
            Un seul outil pour
            <br />
            <span className="bg-gradient-to-r from-accent via-purple-500 to-pink-500 bg-clip-text text-transparent">
              tout coordonner
            </span>
          </h1>

          <p className="text-xl text-gray-400 mb-12 max-w-3xl mx-auto">
            Fini les 10 applications différentes. Hubz centralise vos organisations, équipes, tâches, objectifs et habitudes dans une seule interface moderne et puissante.
          </p>

          <div className="flex items-center justify-center gap-4 mb-16">
            <Button size="lg" onClick={() => navigate('/register')} className="text-lg px-8 py-4">
              Démarrer gratuitement
              <ArrowRight className="w-5 h-5" />
            </Button>
            <Button size="lg" variant="secondary" className="text-lg px-8 py-4">
              Voir la démo
            </Button>
          </div>

          {/* Hero Image / Dashboard Preview */}
          <Card className="max-w-6xl mx-auto p-2 bg-white/5 border-white/10">
            <div className="relative rounded-lg overflow-hidden bg-gradient-to-br from-[#12121A] to-[#1A1A24] aspect-video flex items-center justify-center">
              <div className="absolute inset-0 bg-[url('/grid.svg')] opacity-20"></div>
              <div className="relative z-10 text-gray-500 flex flex-col items-center gap-4">
                <Building2 className="w-20 h-20" />
                <p className="text-lg">Dashboard Preview</p>
              </div>
            </div>
          </Card>
        </div>
      </section>

      {/* Why Hubz - Comparison Section */}
      <section className="py-20 px-6 bg-white/5">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-5xl font-bold text-white mb-6">
              Pourquoi Hubz plutôt que la concurrence ?
            </h2>
            <p className="text-xl text-gray-400">
              Arrêtez de jongler entre plusieurs outils. Voici pourquoi Hubz se démarque.
            </p>
          </div>

          <div className="grid md:grid-cols-2 gap-8 mb-12">
            {/* With Others */}
            <Card className="p-8 bg-red-500/5 border-red-500/20">
              <div className="flex items-center gap-3 mb-6">
                <div className="w-12 h-12 rounded-xl bg-red-500/10 flex items-center justify-center">
                  <X className="w-6 h-6 text-red-500" />
                </div>
                <h3 className="text-2xl font-bold text-white">Autres outils</h3>
              </div>
              <ul className="space-y-4">
                <li className="flex items-start gap-3 text-gray-400">
                  <X className="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" />
                  <span>10+ applications à gérer séparément</span>
                </li>
                <li className="flex items-start gap-3 text-gray-400">
                  <X className="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" />
                  <span>Abonnements multiples qui s'accumulent (50-200€/mois)</span>
                </li>
                <li className="flex items-start gap-3 text-gray-400">
                  <X className="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" />
                  <span>Données éparpillées, impossible à synchroniser</span>
                </li>
                <li className="flex items-start gap-3 text-gray-400">
                  <X className="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" />
                  <span>Courbe d'apprentissage pour chaque outil</span>
                </li>
                <li className="flex items-start gap-3 text-gray-400">
                  <X className="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" />
                  <span>Pas de vue d'ensemble unifiée</span>
                </li>
              </ul>
            </Card>

            {/* With Hubz */}
            <Card className="p-8 bg-accent/5 border-accent/20 ring-2 ring-accent/50">
              <div className="flex items-center gap-3 mb-6">
                <div className="w-12 h-12 rounded-xl bg-accent/10 flex items-center justify-center">
                  <Crown className="w-6 h-6 text-accent" />
                </div>
                <h3 className="text-2xl font-bold text-white">Avec Hubz</h3>
              </div>
              <ul className="space-y-4">
                <li className="flex items-start gap-3 text-gray-200">
                  <CheckCircle2 className="w-5 h-5 text-accent flex-shrink-0 mt-0.5" />
                  <span><strong className="text-white">Une seule plateforme</strong> pour tout gérer</span>
                </li>
                <li className="flex items-start gap-3 text-gray-200">
                  <CheckCircle2 className="w-5 h-5 text-accent flex-shrink-0 mt-0.5" />
                  <span><strong className="text-white">Prix unique et transparent</strong> (dès 9€/mois)</span>
                </li>
                <li className="flex items-start gap-3 text-gray-200">
                  <CheckCircle2 className="w-5 h-5 text-accent flex-shrink-0 mt-0.5" />
                  <span><strong className="text-white">Synchronisation automatique</strong> entre tous vos espaces</span>
                </li>
                <li className="flex items-start gap-3 text-gray-200">
                  <CheckCircle2 className="w-5 h-5 text-accent flex-shrink-0 mt-0.5" />
                  <span><strong className="text-white">Interface unifiée et intuitive</strong> facile à maîtriser</span>
                </li>
                <li className="flex items-start gap-3 text-gray-200">
                  <CheckCircle2 className="w-5 h-5 text-accent flex-shrink-0 mt-0.5" />
                  <span><strong className="text-white">Vue 360° de votre productivité</strong> en temps réel</span>
                </li>
              </ul>
            </Card>
          </div>

          {/* Comparison Table */}
          <Card className="overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-white/5 border-b border-white/10">
                  <tr>
                    <th className="px-6 py-4 text-left text-white font-semibold">Fonctionnalité</th>
                    <th className="px-6 py-4 text-center text-gray-400">Notion</th>
                    <th className="px-6 py-4 text-center text-gray-400">Asana</th>
                    <th className="px-6 py-4 text-center text-gray-400">ClickUp</th>
                    <th className="px-6 py-4 text-center text-accent font-semibold">Hubz</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/10">
                  <tr>
                    <td className="px-6 py-4 text-gray-300">Gestion multi-organisations</td>
                    <td className="px-6 py-4 text-center"><X className="w-5 h-5 text-red-500 mx-auto" /></td>
                    <td className="px-6 py-4 text-center"><Check className="w-5 h-5 text-green-500 mx-auto" /></td>
                    <td className="px-6 py-4 text-center"><Check className="w-5 h-5 text-green-500 mx-auto" /></td>
                    <td className="px-6 py-4 text-center"><Check className="w-5 h-5 text-accent mx-auto" /></td>
                  </tr>
                  <tr>
                    <td className="px-6 py-4 text-gray-300">Gestion d'habitudes intégrée</td>
                    <td className="px-6 py-4 text-center"><X className="w-5 h-5 text-red-500 mx-auto" /></td>
                    <td className="px-6 py-4 text-center"><X className="w-5 h-5 text-red-500 mx-auto" /></td>
                    <td className="px-6 py-4 text-center"><X className="w-5 h-5 text-red-500 mx-auto" /></td>
                    <td className="px-6 py-4 text-center"><Check className="w-5 h-5 text-accent mx-auto" /></td>
                  </tr>
                  <tr>
                    <td className="px-6 py-4 text-gray-300">Objectifs personnels + pro</td>
                    <td className="px-6 py-4 text-center"><X className="w-5 h-5 text-red-500 mx-auto" /></td>
                    <td className="px-6 py-4 text-center"><Check className="w-5 h-5 text-green-500 mx-auto" /></td>
                    <td className="px-6 py-4 text-center"><Check className="w-5 h-5 text-green-500 mx-auto" /></td>
                    <td className="px-6 py-4 text-center"><Check className="w-5 h-5 text-accent mx-auto" /></td>
                  </tr>
                  <tr>
                    <td className="px-6 py-4 text-gray-300">Interface moderne et rapide</td>
                    <td className="px-6 py-4 text-center"><Check className="w-5 h-5 text-green-500 mx-auto" /></td>
                    <td className="px-6 py-4 text-center"><X className="w-5 h-5 text-red-500 mx-auto" /></td>
                    <td className="px-6 py-4 text-center"><X className="w-5 h-5 text-red-500 mx-auto" /></td>
                    <td className="px-6 py-4 text-center"><Check className="w-5 h-5 text-accent mx-auto" /></td>
                  </tr>
                  <tr>
                    <td className="px-6 py-4 text-gray-300">Prix transparent</td>
                    <td className="px-6 py-4 text-center text-gray-400">10€</td>
                    <td className="px-6 py-4 text-center text-gray-400">14€</td>
                    <td className="px-6 py-4 text-center text-gray-400">12€</td>
                    <td className="px-6 py-4 text-center text-accent font-semibold">9€</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </Card>
        </div>
      </section>

      {/* Stats Section - Boost Activity */}
      <section className="py-20 px-6 relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-r from-accent/10 via-purple-500/10 to-pink-500/10"></div>
        <div className="absolute inset-0 bg-[url('/grid.svg')] opacity-10"></div>

        <div className="max-w-7xl mx-auto relative z-10">
          <div className="text-center mb-12">
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-accent/10 border border-accent/20 text-accent mb-4">
              <Zap className="w-4 h-4" />
              <span className="text-sm font-medium">Performances impressionnantes</span>
            </div>
            <h2 className="text-5xl font-bold text-white mb-4">
              Des chiffres qui parlent
            </h2>
            <p className="text-xl text-gray-400">
              Rejoignez une communauté qui ne cesse de grandir
            </p>
          </div>

          <div className="grid md:grid-cols-4 gap-6">
            {/* Stat 1 */}
            <div className="relative group">
              <div className="absolute inset-0 bg-gradient-to-r from-accent to-purple-500 rounded-2xl blur-xl opacity-20 group-hover:opacity-40 transition-opacity"></div>
              <Card className="relative p-8 text-center bg-white/10 border-white/20 backdrop-blur-xl">
                <div className="text-5xl font-bold bg-gradient-to-r from-accent to-purple-500 bg-clip-text text-transparent mb-2">
                  10k+
                </div>
                <p className="text-gray-300 font-medium">Utilisateurs actifs</p>
              </Card>
            </div>

            {/* Stat 2 */}
            <div className="relative group">
              <div className="absolute inset-0 bg-gradient-to-r from-purple-500 to-pink-500 rounded-2xl blur-xl opacity-20 group-hover:opacity-40 transition-opacity"></div>
              <Card className="relative p-8 text-center bg-white/10 border-white/20 backdrop-blur-xl">
                <div className="text-5xl font-bold bg-gradient-to-r from-purple-500 to-pink-500 bg-clip-text text-transparent mb-2">
                  500k+
                </div>
                <p className="text-gray-300 font-medium">Tâches complétées</p>
              </Card>
            </div>

            {/* Stat 3 */}
            <div className="relative group">
              <div className="absolute inset-0 bg-gradient-to-r from-pink-500 to-orange-500 rounded-2xl blur-xl opacity-20 group-hover:opacity-40 transition-opacity"></div>
              <Card className="relative p-8 text-center bg-white/10 border-white/20 backdrop-blur-xl">
                <div className="text-5xl font-bold bg-gradient-to-r from-pink-500 to-orange-500 bg-clip-text text-transparent mb-2">
                  85%
                </div>
                <p className="text-gray-300 font-medium">Gain de productivité</p>
              </Card>
            </div>

            {/* Stat 4 */}
            <div className="relative group">
              <div className="absolute inset-0 bg-gradient-to-r from-green-500 to-blue-500 rounded-2xl blur-xl opacity-20 group-hover:opacity-40 transition-opacity"></div>
              <Card className="relative p-8 text-center bg-white/10 border-white/20 backdrop-blur-xl">
                <div className="text-5xl font-bold bg-gradient-to-r from-green-500 to-blue-500 bg-clip-text text-transparent mb-2">
                  4.9/5
                </div>
                <p className="text-gray-300 font-medium">Note moyenne</p>
              </Card>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section - Ultra Futuristic */}
      <section className="py-20 px-6 relative overflow-hidden">
        <div className="absolute inset-0">
          <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-accent/20 rounded-full blur-[120px] animate-pulse"></div>
          <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-purple-500/20 rounded-full blur-[120px] animate-pulse delay-1000"></div>
        </div>

        <div className="max-w-7xl mx-auto relative z-10">
          <div className="text-center mb-16">
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-gradient-to-r from-accent/10 to-purple-500/10 border border-accent/20 text-accent mb-6">
              <Sparkles className="w-4 h-4 animate-pulse" />
              <span className="text-sm font-medium">Fonctionnalités de pointe</span>
            </div>
            <h2 className="text-6xl font-bold text-white mb-6">
              Une plateforme
              <br />
              <span className="bg-gradient-to-r from-accent via-purple-500 to-pink-500 bg-clip-text text-transparent">
                ultra-performante
              </span>
            </h2>
            <p className="text-xl text-gray-400 max-w-3xl mx-auto">
              Découvrez les fonctionnalités qui vont révolutionner votre façon de travailler
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {/* Feature 1 - Multi-organisations */}
            <div className="group relative">
              <div className="absolute -inset-0.5 bg-gradient-to-r from-accent to-purple-600 rounded-2xl blur opacity-0 group-hover:opacity-75 transition duration-500"></div>
              <Card className="relative p-8 bg-[#12121A]/80 border-white/10 backdrop-blur-xl hover:bg-[#1A1A24]/90 transition-all duration-300 h-full">
                <div className="relative w-16 h-16 mb-6">
                  <div className="absolute inset-0 bg-gradient-to-br from-accent to-purple-600 rounded-xl animate-pulse"></div>
                  <div className="absolute inset-0 bg-gradient-to-br from-accent to-purple-600 rounded-xl blur-md"></div>
                  <div className="relative w-full h-full bg-[#12121A] rounded-xl flex items-center justify-center">
                    <Building2 className="w-8 h-8 text-accent" />
                  </div>
                </div>
                <h3 className="text-2xl font-bold text-white mb-3 group-hover:text-transparent group-hover:bg-gradient-to-r group-hover:from-accent group-hover:to-purple-500 group-hover:bg-clip-text transition-all">
                  Multi-organisations
                </h3>
                <p className="text-gray-400 leading-relaxed">
                  Gérez simultanément plusieurs organisations avec équipes, rôles et permissions ultra-personnalisés
                </p>
                <div className="mt-6 flex items-center gap-2 text-accent font-medium opacity-0 group-hover:opacity-100 transition-opacity">
                  <span>En savoir plus</span>
                  <ArrowRight className="w-4 h-4" />
                </div>
              </Card>
            </div>

            {/* Feature 2 - Gestion de tâches */}
            <div className="group relative">
              <div className="absolute -inset-0.5 bg-gradient-to-r from-purple-500 to-pink-500 rounded-2xl blur opacity-0 group-hover:opacity-75 transition duration-500"></div>
              <Card className="relative p-8 bg-[#12121A]/80 border-white/10 backdrop-blur-xl hover:bg-[#1A1A24]/90 transition-all duration-300 h-full">
                <div className="relative w-16 h-16 mb-6">
                  <div className="absolute inset-0 bg-gradient-to-br from-purple-500 to-pink-500 rounded-xl animate-pulse"></div>
                  <div className="absolute inset-0 bg-gradient-to-br from-purple-500 to-pink-500 rounded-xl blur-md"></div>
                  <div className="relative w-full h-full bg-[#12121A] rounded-xl flex items-center justify-center">
                    <Zap className="w-8 h-8 text-purple-500" />
                  </div>
                </div>
                <h3 className="text-2xl font-bold text-white mb-3 group-hover:text-transparent group-hover:bg-gradient-to-r group-hover:from-purple-500 group-hover:to-pink-500 group-hover:bg-clip-text transition-all">
                  Gestion de tâches
                </h3>
                <p className="text-gray-400 leading-relaxed">
                  Kanban ultra-intuitif avec assignations intelligentes, priorités dynamiques et statuts avancés
                </p>
                <div className="mt-6 flex items-center gap-2 text-purple-500 font-medium opacity-0 group-hover:opacity-100 transition-opacity">
                  <span>En savoir plus</span>
                  <ArrowRight className="w-4 h-4" />
                </div>
              </Card>
            </div>

            {/* Feature 3 - Équipes collaboratives */}
            <div className="group relative">
              <div className="absolute -inset-0.5 bg-gradient-to-r from-pink-500 to-orange-500 rounded-2xl blur opacity-0 group-hover:opacity-75 transition duration-500"></div>
              <Card className="relative p-8 bg-[#12121A]/80 border-white/10 backdrop-blur-xl hover:bg-[#1A1A24]/90 transition-all duration-300 h-full">
                <div className="relative w-16 h-16 mb-6">
                  <div className="absolute inset-0 bg-gradient-to-br from-pink-500 to-orange-500 rounded-xl animate-pulse"></div>
                  <div className="absolute inset-0 bg-gradient-to-br from-pink-500 to-orange-500 rounded-xl blur-md"></div>
                  <div className="relative w-full h-full bg-[#12121A] rounded-xl flex items-center justify-center">
                    <Users className="w-8 h-8 text-pink-500" />
                  </div>
                </div>
                <h3 className="text-2xl font-bold text-white mb-3 group-hover:text-transparent group-hover:bg-gradient-to-r group-hover:from-pink-500 group-hover:to-orange-500 group-hover:bg-clip-text transition-all">
                  Collaboration temps réel
                </h3>
                <p className="text-gray-400 leading-relaxed">
                  Créez des équipes performantes et collaborez instantanément avec synchronisation en temps réel
                </p>
                <div className="mt-6 flex items-center gap-2 text-pink-500 font-medium opacity-0 group-hover:opacity-100 transition-opacity">
                  <span>En savoir plus</span>
                  <ArrowRight className="w-4 h-4" />
                </div>
              </Card>
            </div>

            {/* Feature 4 - Objectifs & OKRs */}
            <div className="group relative">
              <div className="absolute -inset-0.5 bg-gradient-to-r from-green-500 to-emerald-500 rounded-2xl blur opacity-0 group-hover:opacity-75 transition duration-500"></div>
              <Card className="relative p-8 bg-[#12121A]/80 border-white/10 backdrop-blur-xl hover:bg-[#1A1A24]/90 transition-all duration-300 h-full">
                <div className="relative w-16 h-16 mb-6">
                  <div className="absolute inset-0 bg-gradient-to-br from-green-500 to-emerald-500 rounded-xl animate-pulse"></div>
                  <div className="absolute inset-0 bg-gradient-to-br from-green-500 to-emerald-500 rounded-xl blur-md"></div>
                  <div className="relative w-full h-full bg-[#12121A] rounded-xl flex items-center justify-center">
                    <Target className="w-8 h-8 text-green-500" />
                  </div>
                </div>
                <h3 className="text-2xl font-bold text-white mb-3 group-hover:text-transparent group-hover:bg-gradient-to-r group-hover:from-green-500 group-hover:to-emerald-500 group-hover:bg-clip-text transition-all">
                  Objectifs & OKRs
                </h3>
                <p className="text-gray-400 leading-relaxed">
                  Définissez et suivez vos objectifs court, moyen et long terme avec analytics avancés
                </p>
                <div className="mt-6 flex items-center gap-2 text-green-500 font-medium opacity-0 group-hover:opacity-100 transition-opacity">
                  <span>En savoir plus</span>
                  <ArrowRight className="w-4 h-4" />
                </div>
              </Card>
            </div>

            {/* Feature 5 - Calendrier unifié */}
            <div className="group relative">
              <div className="absolute -inset-0.5 bg-gradient-to-r from-orange-500 to-red-500 rounded-2xl blur opacity-0 group-hover:opacity-75 transition duration-500"></div>
              <Card className="relative p-8 bg-[#12121A]/80 border-white/10 backdrop-blur-xl hover:bg-[#1A1A24]/90 transition-all duration-300 h-full">
                <div className="relative w-16 h-16 mb-6">
                  <div className="absolute inset-0 bg-gradient-to-br from-orange-500 to-red-500 rounded-xl animate-pulse"></div>
                  <div className="absolute inset-0 bg-gradient-to-br from-orange-500 to-red-500 rounded-xl blur-md"></div>
                  <div className="relative w-full h-full bg-[#12121A] rounded-xl flex items-center justify-center">
                    <Calendar className="w-8 h-8 text-orange-500" />
                  </div>
                </div>
                <h3 className="text-2xl font-bold text-white mb-3 group-hover:text-transparent group-hover:bg-gradient-to-r group-hover:from-orange-500 group-hover:to-red-500 group-hover:bg-clip-text transition-all">
                  Calendrier intelligent
                </h3>
                <p className="text-gray-400 leading-relaxed">
                  Vue unifiée de tous vos événements avec suggestions intelligentes et synchronisation multi-plateformes
                </p>
                <div className="mt-6 flex items-center gap-2 text-orange-500 font-medium opacity-0 group-hover:opacity-100 transition-opacity">
                  <span>En savoir plus</span>
                  <ArrowRight className="w-4 h-4" />
                </div>
              </Card>
            </div>

            {/* Feature 6 - Suivi d'habitudes */}
            <div className="group relative">
              <div className="absolute -inset-0.5 bg-gradient-to-r from-blue-500 to-cyan-500 rounded-2xl blur opacity-0 group-hover:opacity-75 transition duration-500"></div>
              <Card className="relative p-8 bg-[#12121A]/80 border-white/10 backdrop-blur-xl hover:bg-[#1A1A24]/90 transition-all duration-300 h-full">
                <div className="relative w-16 h-16 mb-6">
                  <div className="absolute inset-0 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-xl animate-pulse"></div>
                  <div className="absolute inset-0 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-xl blur-md"></div>
                  <div className="relative w-full h-full bg-[#12121A] rounded-xl flex items-center justify-center">
                    <TrendingUp className="w-8 h-8 text-blue-500" />
                  </div>
                </div>
                <h3 className="text-2xl font-bold text-white mb-3 group-hover:text-transparent group-hover:bg-gradient-to-r group-hover:from-blue-500 group-hover:to-cyan-500 group-hover:bg-clip-text transition-all">
                  Suivi d'habitudes
                </h3>
                <p className="text-gray-400 leading-relaxed">
                  Transformez vos habitudes avec tracking quotidien, streaks motivants et statistiques détaillées
                </p>
                <div className="mt-6 flex items-center gap-2 text-blue-500 font-medium opacity-0 group-hover:opacity-100 transition-opacity">
                  <span>En savoir plus</span>
                  <ArrowRight className="w-4 h-4" />
                </div>
              </Card>
            </div>
          </div>

          {/* Bonus Features Row */}
          <div className="grid md:grid-cols-3 gap-6 mt-6">
            {/* Feature 7 - Sécurité */}
            <div className="group relative">
              <div className="absolute -inset-0.5 bg-gradient-to-r from-yellow-500 to-amber-500 rounded-2xl blur opacity-0 group-hover:opacity-75 transition duration-500"></div>
              <Card className="relative p-6 bg-[#12121A]/80 border-white/10 backdrop-blur-xl hover:bg-[#1A1A24]/90 transition-all duration-300 h-full flex items-center gap-4">
                <div className="relative w-12 h-12 flex-shrink-0">
                  <div className="absolute inset-0 bg-gradient-to-br from-yellow-500 to-amber-500 rounded-lg opacity-50 blur-md"></div>
                  <div className="relative w-full h-full bg-[#12121A] rounded-lg flex items-center justify-center">
                    <Shield className="w-6 h-6 text-yellow-500" />
                  </div>
                </div>
                <div>
                  <h3 className="text-lg font-bold text-white mb-1 group-hover:text-yellow-500 transition-colors">
                    Sécurité militaire
                  </h3>
                  <p className="text-sm text-gray-400">Chiffrement end-to-end et JWT avancé</p>
                </div>
              </Card>
            </div>

            {/* Feature 8 - Espace personnel */}
            <div className="group relative">
              <div className="absolute -inset-0.5 bg-gradient-to-r from-indigo-500 to-purple-500 rounded-2xl blur opacity-0 group-hover:opacity-75 transition duration-500"></div>
              <Card className="relative p-6 bg-[#12121A]/80 border-white/10 backdrop-blur-xl hover:bg-[#1A1A24]/90 transition-all duration-300 h-full flex items-center gap-4">
                <div className="relative w-12 h-12 flex-shrink-0">
                  <div className="absolute inset-0 bg-gradient-to-br from-indigo-500 to-purple-500 rounded-lg opacity-50 blur-md"></div>
                  <div className="relative w-full h-full bg-[#12121A] rounded-lg flex items-center justify-center">
                    <User className="w-6 h-6 text-indigo-500" />
                  </div>
                </div>
                <div>
                  <h3 className="text-lg font-bold text-white mb-1 group-hover:text-indigo-500 transition-colors">
                    Espace personnel
                  </h3>
                  <p className="text-sm text-gray-400">Vie privée et pro parfaitement séparées</p>
                </div>
              </Card>
            </div>

            {/* Feature 9 - Interface */}
            <div className="group relative">
              <div className="absolute -inset-0.5 bg-gradient-to-r from-rose-500 to-pink-500 rounded-2xl blur opacity-0 group-hover:opacity-75 transition duration-500"></div>
              <Card className="relative p-6 bg-[#12121A]/80 border-white/10 backdrop-blur-xl hover:bg-[#1A1A24]/90 transition-all duration-300 h-full flex items-center gap-4">
                <div className="relative w-12 h-12 flex-shrink-0">
                  <div className="absolute inset-0 bg-gradient-to-br from-rose-500 to-pink-500 rounded-lg opacity-50 blur-md"></div>
                  <div className="relative w-full h-full bg-[#12121A] rounded-lg flex items-center justify-center">
                    <Sparkles className="w-6 h-6 text-rose-500" />
                  </div>
                </div>
                <div>
                  <h3 className="text-lg font-bold text-white mb-1 group-hover:text-rose-500 transition-colors">
                    Design premium
                  </h3>
                  <p className="text-sm text-gray-400">Glassmorphism et animations de pointe</p>
                </div>
              </Card>
            </div>
          </div>
        </div>
      </section>

      {/* Pricing Section */}
      <section className="py-20 px-6 bg-white/5">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-5xl font-bold text-white mb-6">
              Tarification simple et transparente
            </h2>
            <p className="text-xl text-gray-400">
              Choisissez le plan qui vous convient. Changez à tout moment.
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-8 max-w-6xl mx-auto">
            {/* Free Plan */}
            <Card className="p-8">
              <h3 className="text-2xl font-bold text-white mb-2">Gratuit</h3>
              <div className="mb-6">
                <span className="text-5xl font-bold text-white">0€</span>
                <span className="text-gray-400">/mois</span>
              </div>
              <ul className="space-y-3 mb-8">
                <li className="flex items-center gap-3 text-gray-300">
                  <Check className="w-5 h-5 text-green-500" />
                  <span>1 organisation</span>
                </li>
                <li className="flex items-center gap-3 text-gray-300">
                  <Check className="w-5 h-5 text-green-500" />
                  <span>Jusqu'à 5 membres</span>
                </li>
                <li className="flex items-center gap-3 text-gray-300">
                  <Check className="w-5 h-5 text-green-500" />
                  <span>Fonctionnalités de base</span>
                </li>
                <li className="flex items-center gap-3 text-gray-300">
                  <Check className="w-5 h-5 text-green-500" />
                  <span>Espace personnel illimité</span>
                </li>
              </ul>
              <Button variant="secondary" className="w-full" onClick={() => navigate('/register')}>
                Commencer gratuitement
              </Button>
            </Card>

            {/* Pro Plan */}
            <Card className="p-8 ring-2 ring-accent relative">
              <div className="absolute -top-4 left-1/2 -translate-x-1/2 px-4 py-1 bg-accent rounded-full text-sm font-semibold text-white">
                Populaire
              </div>
              <h3 className="text-2xl font-bold text-white mb-2">Pro</h3>
              <div className="mb-6">
                <span className="text-5xl font-bold text-white">9€</span>
                <span className="text-gray-400">/mois</span>
              </div>
              <ul className="space-y-3 mb-8">
                <li className="flex items-center gap-3 text-gray-300">
                  <Check className="w-5 h-5 text-accent" />
                  <span><strong className="text-white">Organisations illimitées</strong></span>
                </li>
                <li className="flex items-center gap-3 text-gray-300">
                  <Check className="w-5 h-5 text-accent" />
                  <span><strong className="text-white">Membres illimités</strong></span>
                </li>
                <li className="flex items-center gap-3 text-gray-300">
                  <Check className="w-5 h-5 text-accent" />
                  <span>Toutes les fonctionnalités</span>
                </li>
                <li className="flex items-center gap-3 text-gray-300">
                  <Check className="w-5 h-5 text-accent" />
                  <span>Support prioritaire</span>
                </li>
                <li className="flex items-center gap-3 text-gray-300">
                  <Check className="w-5 h-5 text-accent" />
                  <span>Statistiques avancées</span>
                </li>
              </ul>
              <Button className="w-full" onClick={() => navigate('/register')}>
                Commencer l'essai
                <ArrowRight className="w-4 h-4" />
              </Button>
            </Card>

            {/* Enterprise Plan */}
            <Card className="p-8">
              <h3 className="text-2xl font-bold text-white mb-2">Entreprise</h3>
              <div className="mb-6">
                <span className="text-5xl font-bold text-white">Sur mesure</span>
              </div>
              <ul className="space-y-3 mb-8">
                <li className="flex items-center gap-3 text-gray-300">
                  <Check className="w-5 h-5 text-green-500" />
                  <span>Tout du plan Pro</span>
                </li>
                <li className="flex items-center gap-3 text-gray-300">
                  <Check className="w-5 h-5 text-green-500" />
                  <span>SSO & SAML</span>
                </li>
                <li className="flex items-center gap-3 text-gray-300">
                  <Check className="w-5 h-5 text-green-500" />
                  <span>Audit logs</span>
                </li>
                <li className="flex items-center gap-3 text-gray-300">
                  <Check className="w-5 h-5 text-green-500" />
                  <span>Support dédié 24/7</span>
                </li>
                <li className="flex items-center gap-3 text-gray-300">
                  <Check className="w-5 h-5 text-green-500" />
                  <span>Formation personnalisée</span>
                </li>
              </ul>
              <Button variant="secondary" className="w-full">
                Nous contacter
              </Button>
            </Card>
          </div>
        </div>
      </section>

      {/* Testimonials Section */}
      <section className="py-20 px-6">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-5xl font-bold text-white mb-6">
              Ils ont fait le choix Hubz
            </h2>
            <p className="text-xl text-gray-400">
              Rejoignez des milliers d'équipes qui ont transformé leur productivité
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-8">
            {/* Testimonial 1 */}
            <Card className="p-6">
              <div className="flex gap-1 mb-4">
                {[...Array(5)].map((_, i) => (
                  <Star key={i} className="w-5 h-5 text-yellow-500 fill-yellow-500" />
                ))}
              </div>
              <p className="text-gray-300 mb-6">
                "Hubz a complètement transformé notre façon de travailler. Fini le chaos entre Notion, Asana et Slack. Tout est centralisé et notre productivité a explosé."
              </p>
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-gradient-to-br from-accent to-purple-500"></div>
                <div>
                  <p className="font-semibold text-white">Marie Dubois</p>
                  <p className="text-sm text-gray-400">CEO @ TechStart</p>
                </div>
              </div>
            </Card>

            {/* Testimonial 2 */}
            <Card className="p-6">
              <div className="flex gap-1 mb-4">
                {[...Array(5)].map((_, i) => (
                  <Star key={i} className="w-5 h-5 text-yellow-500 fill-yellow-500" />
                ))}
              </div>
              <p className="text-gray-300 mb-6">
                "L'interface est magnifique et intuitive. On a migré toute l'équipe en une journée. Le suivi d'habitudes personnelles est un vrai plus pour l'équilibre vie pro/perso."
              </p>
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-gradient-to-br from-pink-500 to-orange-500"></div>
                <div>
                  <p className="font-semibold text-white">Thomas Martin</p>
                  <p className="text-sm text-gray-400">Product Manager @ Digital Co</p>
                </div>
              </div>
            </Card>

            {/* Testimonial 3 */}
            <Card className="p-6">
              <div className="flex gap-1 mb-4">
                {[...Array(5)].map((_, i) => (
                  <Star key={i} className="w-5 h-5 text-yellow-500 fill-yellow-500" />
                ))}
              </div>
              <p className="text-gray-300 mb-6">
                "Enfin un outil qui comprend comment fonctionnent vraiment les équipes modernes. La gestion multi-organisations est parfaite pour nos différents clients."
              </p>
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-gradient-to-br from-green-500 to-blue-500"></div>
                <div>
                  <p className="font-semibold text-white">Sophie Laurent</p>
                  <p className="text-sm text-gray-400">Freelance Designer</p>
                </div>
              </div>
            </Card>
          </div>
        </div>
      </section>

      {/* Final CTA Section */}
      <section className="py-20 px-6 bg-gradient-to-r from-accent/20 via-purple-500/20 to-pink-500/20">
        <div className="max-w-4xl mx-auto text-center">
          <h2 className="text-5xl font-bold text-white mb-6">
            Prêt à booster votre productivité ?
          </h2>
          <p className="text-xl text-gray-300 mb-12">
            Rejoignez des milliers d'utilisateurs qui ont simplifié leur organisation. Essai gratuit, sans carte bancaire.
          </p>
          <div className="flex items-center justify-center gap-4">
            <Button size="lg" onClick={() => navigate('/register')} className="text-lg px-8 py-4">
              Démarrer gratuitement
              <ArrowRight className="w-5 h-5" />
            </Button>
            <Button size="lg" variant="secondary" className="text-lg px-8 py-4">
              Planifier une démo
            </Button>
          </div>
          <p className="text-sm text-gray-400 mt-6">
            ✓ Gratuit pour toujours • ✓ Sans carte bancaire • ✓ Configuration en 2 minutes
          </p>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-12 px-6 border-t border-white/10">
        <div className="max-w-7xl mx-auto">
          <div className="grid md:grid-cols-4 gap-8 mb-8">
            <div>
              <div className="flex items-center gap-2 mb-4">
                <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-accent to-purple-600 flex items-center justify-center">
                  <Sparkles className="w-5 h-5 text-white" />
                </div>
                <span className="text-xl font-bold text-white">Hubz</span>
              </div>
              <p className="text-gray-400 text-sm">
                La plateforme tout-en-un pour gérer vos organisations, équipes et objectifs.
              </p>
            </div>

            <div>
              <h4 className="font-semibold text-white mb-4">Produit</h4>
              <ul className="space-y-2 text-sm text-gray-400">
                <li><a href="#" className="hover:text-accent transition-colors">Fonctionnalités</a></li>
                <li><a href="#" className="hover:text-accent transition-colors">Tarifs</a></li>
                <li><a href="#" className="hover:text-accent transition-colors">Sécurité</a></li>
                <li><a href="#" className="hover:text-accent transition-colors">Roadmap</a></li>
              </ul>
            </div>

            <div>
              <h4 className="font-semibold text-white mb-4">Ressources</h4>
              <ul className="space-y-2 text-sm text-gray-400">
                <li><a href="#" className="hover:text-accent transition-colors">Documentation</a></li>
                <li><a href="#" className="hover:text-accent transition-colors">Blog</a></li>
                <li><a href="#" className="hover:text-accent transition-colors">Guides</a></li>
                <li><a href="#" className="hover:text-accent transition-colors">Support</a></li>
              </ul>
            </div>

            <div>
              <h4 className="font-semibold text-white mb-4">Entreprise</h4>
              <ul className="space-y-2 text-sm text-gray-400">
                <li><a href="#" className="hover:text-accent transition-colors">À propos</a></li>
                <li><a href="#" className="hover:text-accent transition-colors">Nous contacter</a></li>
                <li><a href="#" className="hover:text-accent transition-colors">Confidentialité</a></li>
                <li><a href="#" className="hover:text-accent transition-colors">CGU</a></li>
              </ul>
            </div>
          </div>

          <div className="pt-8 border-t border-white/10 flex flex-col md:flex-row items-center justify-between gap-4">
            <p className="text-sm text-gray-400">
              © 2026 Hubz. Tous droits réservés.
            </p>
            <div className="flex items-center gap-6 text-sm text-gray-400">
              <a href="#" className="hover:text-accent transition-colors">Twitter</a>
              <a href="#" className="hover:text-accent transition-colors">LinkedIn</a>
              <a href="#" className="hover:text-accent transition-colors">GitHub</a>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
