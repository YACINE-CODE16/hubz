import { useEffect, useState, useCallback } from 'react';
import { LogOut, Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import { useAuth } from '../../hooks/useAuth';
import { organizationService } from '../../services/organization.service';
import type { Organization } from '../../types/organization';
import Button from '../../components/ui/Button';
import Card from '../../components/ui/Card';
import SpaceCard from '../../components/features/SpaceCard';
import CreateOrgModal from '../../components/features/CreateOrgModal';

export default function HubPage() {
  const { user, logout } = useAuth();
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [showCreateModal, setShowCreateModal] = useState(false);

  const fetchOrganizations = useCallback(async () => {
    try {
      const data = await organizationService.getAll();
      setOrganizations(data);
    } catch (error) {
      toast.error('Erreur lors du chargement des organisations');
      console.error(error);
    }
  }, []);

  useEffect(() => {
    fetchOrganizations();
  }, [fetchOrganizations]);

  const handleCreateOrg = async (data: {
    name: string;
    description?: string;
    icon: string;
    color: string;
  }) => {
    await organizationService.create(data);
    await fetchOrganizations();
  };

  return (
    <div className="min-h-screen bg-light-base dark:bg-dark-base">
      {/* Header */}
      <header className="border-b border-gray-200/50 dark:border-white/10 bg-light-card/50 dark:bg-dark-card/50 backdrop-blur-sm">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
          <h1 className="text-xl font-bold text-accent">Hubz</h1>
          <div className="flex items-center gap-4">
            <span className="text-sm text-gray-600 dark:text-gray-400">
              {user?.firstName} {user?.lastName}
            </span>
            <Button variant="ghost" size="sm" onClick={logout}>
              <LogOut className="h-4 w-4" />
              Déconnexion
            </Button>
          </div>
        </div>
      </header>

      {/* Content */}
      <main className="mx-auto max-w-5xl px-6 py-8">
        {/* Greeting */}
        <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
          Bonjour {user?.firstName}
        </h2>

        {/* Spaces */}
        <section className="mt-8">
          <h3 className="mb-4 text-lg font-semibold text-gray-800 dark:text-gray-200">
            Mes espaces
          </h3>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {/* Personal space — always present */}
            <SpaceCard
              name="Mon espace perso"
              description="Mes projets et fichiers personnels"
              icon="star"
              color="#6366F1"
              to="/personal"
            />

            {/* Organizations */}
            {organizations.map((org) => (
              <SpaceCard
                key={org.id}
                name={org.name}
                description={org.description}
                icon={org.icon}
                color={org.color}
                to={`/organization/${org.id}/tasks`}
              />
            ))}

            {/* Add org */}
            <Card
              onClick={() => setShowCreateModal(true)}
              className="flex cursor-pointer items-center justify-center gap-2 p-5 text-gray-400 dark:text-gray-500 transition-all hover:scale-[1.02] hover:text-accent dark:hover:text-accent hover:shadow-md"
            >
              <Plus className="h-5 w-5" />
              <span className="text-sm font-medium">Nouvelle organisation</span>
            </Card>
          </div>
        </section>
      </main>

      <CreateOrgModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSubmit={handleCreateOrg}
      />
    </div>
  );
}
