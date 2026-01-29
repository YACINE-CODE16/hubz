import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Users, Plus, Edit2, Trash2, UserPlus, UserMinus } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Modal from '../../components/ui/Modal';
import Input from '../../components/ui/Input';
import { teamService } from '../../services/team.service';
import { organizationService } from '../../services/organization.service';
import type { Team, TeamMember, CreateTeamRequest, UpdateTeamRequest } from '../../types/team';
import type { Member } from '../../types/organization';

export default function TeamsPage() {
  const { orgId } = useParams<{ orgId: string }>();
  const [teams, setTeams] = useState<Team[]>([]);
  const [loading, setLoading] = useState(true);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isMembersModalOpen, setIsMembersModalOpen] = useState(false);
  const [selectedTeam, setSelectedTeam] = useState<Team | null>(null);

  useEffect(() => {
    fetchTeams();
  }, [orgId]);

  const fetchTeams = async () => {
    if (!orgId) return;
    try {
      setLoading(true);
      const data = await teamService.getByOrganization(orgId);
      setTeams(data);
    } catch (error) {
      toast.error('Erreur lors du chargement des équipes');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (data: CreateTeamRequest) => {
    if (!orgId) return;
    try {
      await teamService.create(orgId, data);
      toast.success('Équipe créée');
      setIsCreateModalOpen(false);
      fetchTeams();
    } catch (error) {
      toast.error('Erreur lors de la création');
    }
  };

  const handleUpdate = async (data: UpdateTeamRequest) => {
    if (!selectedTeam) return;
    try {
      await teamService.update(selectedTeam.id, data);
      toast.success('Équipe mise à jour');
      setIsEditModalOpen(false);
      setSelectedTeam(null);
      fetchTeams();
    } catch (error) {
      toast.error('Erreur lors de la mise à jour');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Supprimer cette équipe ?')) return;
    try {
      await teamService.delete(id);
      toast.success('Équipe supprimée');
      fetchTeams();
    } catch (error) {
      toast.error('Erreur lors de la suppression');
    }
  };

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-gray-500 dark:text-gray-400">Chargement...</div>
      </div>
    );
  }

  return (
    <div className="flex h-full flex-col gap-6 overflow-auto p-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Équipes</h2>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Gérez les équipes de votre organisation
          </p>
        </div>
        <Button onClick={() => setIsCreateModalOpen(true)}>
          <Plus className="h-4 w-4" />
          Nouvelle équipe
        </Button>
      </div>

      {/* Teams Grid */}
      {teams.length === 0 ? (
        <Card className="flex flex-col items-center justify-center p-12">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
            <Users className="h-8 w-8 text-gray-400" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
            Aucune équipe
          </h3>
          <p className="mt-2 text-center text-sm text-gray-500 dark:text-gray-400">
            Commencez par créer votre première équipe.
          </p>
          <Button onClick={() => setIsCreateModalOpen(true)} className="mt-4">
            <Plus className="h-4 w-4" />
            Créer une équipe
          </Button>
        </Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {teams.map((team) => (
            <TeamCard
              key={team.id}
              team={team}
              onEdit={(team) => {
                setSelectedTeam(team);
                setIsEditModalOpen(true);
              }}
              onManageMembers={(team) => {
                setSelectedTeam(team);
                setIsMembersModalOpen(true);
              }}
              onDelete={handleDelete}
            />
          ))}
        </div>
      )}

      {/* Create Modal */}
      <CreateTeamModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onCreate={handleCreate}
      />

      {/* Edit Modal */}
      {selectedTeam && (
        <EditTeamModal
          isOpen={isEditModalOpen}
          onClose={() => {
            setIsEditModalOpen(false);
            setSelectedTeam(null);
          }}
          onUpdate={handleUpdate}
          team={selectedTeam}
        />
      )}

      {/* Members Modal */}
      {selectedTeam && orgId && (
        <TeamMembersModal
          isOpen={isMembersModalOpen}
          onClose={() => {
            setIsMembersModalOpen(false);
            setSelectedTeam(null);
          }}
          team={selectedTeam}
          orgId={orgId}
          onUpdate={fetchTeams}
        />
      )}
    </div>
  );
}

interface TeamCardProps {
  team: Team;
  onEdit: (team: Team) => void;
  onManageMembers: (team: Team) => void;
  onDelete: (id: string) => void;
}

function TeamCard({ team, onEdit, onManageMembers, onDelete }: TeamCardProps) {
  return (
    <Card className="flex flex-col gap-3">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <h3 className="font-semibold text-gray-900 dark:text-gray-100">{team.name}</h3>
          {team.description && (
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400 line-clamp-2">
              {team.description}
            </p>
          )}
        </div>
        <div className="flex gap-1">
          <button
            onClick={() => onEdit(team)}
            className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600 dark:hover:bg-gray-800 dark:hover:text-gray-300"
            title="Modifier"
          >
            <Edit2 className="h-4 w-4" />
          </button>
          <button
            onClick={() => onDelete(team.id)}
            className="rounded-lg p-1.5 text-gray-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-900/20 dark:hover:text-red-400"
            title="Supprimer"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Footer */}
      <div className="flex items-center justify-between border-t border-gray-200 dark:border-gray-700 pt-3">
        <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
          <Users className="h-4 w-4" />
          <span>{team.memberCount} membre{team.memberCount !== 1 ? 's' : ''}</span>
        </div>
        <button
          onClick={() => onManageMembers(team)}
          className="flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-medium text-accent hover:bg-accent/10"
        >
          <UserPlus className="h-3 w-3" />
          Gérer
        </button>
      </div>
    </Card>
  );
}

interface CreateTeamModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (data: CreateTeamRequest) => void;
}

function CreateTeamModal({ isOpen, onClose, onCreate }: CreateTeamModalProps) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    onCreate({
      name: name.trim(),
      description: description.trim() || undefined,
    });

    // Reset form
    setName('');
    setDescription('');
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Nouvelle équipe">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Nom"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Ex: Développement, Marketing..."
          required
        />

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Description (optionnel)
          </label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Description de l'équipe..."
            rows={3}
            className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 placeholder-gray-400 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent dark:border-gray-600 dark:bg-dark-card dark:text-gray-100 dark:placeholder-gray-500"
          />
        </div>

        <div className="flex gap-2">
          <Button type="button" variant="secondary" onClick={onClose} className="flex-1">
            Annuler
          </Button>
          <Button type="submit" className="flex-1">
            Créer
          </Button>
        </div>
      </form>
    </Modal>
  );
}

interface EditTeamModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUpdate: (data: UpdateTeamRequest) => void;
  team: Team;
}

function EditTeamModal({ isOpen, onClose, onUpdate, team }: EditTeamModalProps) {
  const [name, setName] = useState(team.name);
  const [description, setDescription] = useState(team.description || '');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    onUpdate({
      name: name.trim(),
      description: description.trim() || undefined,
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Modifier l'équipe">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input label="Nom" value={name} onChange={(e) => setName(e.target.value)} required />

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Description (optionnel)
          </label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
            className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 placeholder-gray-400 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent dark:border-gray-600 dark:bg-dark-card dark:text-gray-100 dark:placeholder-gray-500"
          />
        </div>

        <div className="flex gap-2">
          <Button type="button" variant="secondary" onClick={onClose} className="flex-1">
            Annuler
          </Button>
          <Button type="submit" className="flex-1">
            Enregistrer
          </Button>
        </div>
      </form>
    </Modal>
  );
}

interface TeamMembersModalProps {
  isOpen: boolean;
  onClose: () => void;
  team: Team;
  orgId: string;
  onUpdate: () => void;
}

function TeamMembersModal({ isOpen, onClose, team, orgId, onUpdate }: TeamMembersModalProps) {
  const [teamMembers, setTeamMembers] = useState<TeamMember[]>([]);
  const [orgMembers, setOrgMembers] = useState<Member[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (isOpen) {
      fetchData();
    }
  }, [isOpen, team.id]);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [members, allMembers] = await Promise.all([
        teamService.getMembers(team.id),
        organizationService.getMembers(orgId),
      ]);
      setTeamMembers(members);
      setOrgMembers(allMembers);
    } catch (error) {
      toast.error('Erreur lors du chargement');
    } finally {
      setLoading(false);
    }
  };

  const handleAddMember = async (userId: string) => {
    try {
      await teamService.addMember(team.id, userId);
      toast.success('Membre ajouté');
      fetchData();
      onUpdate();
    } catch (error) {
      toast.error('Erreur lors de l\'ajout');
    }
  };

  const handleRemoveMember = async (userId: string) => {
    if (!confirm('Retirer ce membre de l\'équipe ?')) return;
    try {
      await teamService.removeMember(team.id, userId);
      toast.success('Membre retiré');
      fetchData();
      onUpdate();
    } catch (error) {
      toast.error('Erreur lors du retrait');
    }
  };

  const availableMembers = orgMembers.filter(
    (orgMember) => !teamMembers.some((tm) => tm.userId === orgMember.userId)
  );

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Membres de ${team.name}`}>
      <div className="space-y-6">
        {loading ? (
          <div className="flex items-center justify-center py-8">
            <div className="text-gray-500 dark:text-gray-400">Chargement...</div>
          </div>
        ) : (
          <>
            {/* Current Members */}
            <div>
              <h4 className="mb-3 text-sm font-semibold text-gray-900 dark:text-gray-100">
                Membres actuels ({teamMembers.length})
              </h4>
              {teamMembers.length === 0 ? (
                <p className="text-sm text-gray-500 dark:text-gray-400">Aucun membre</p>
              ) : (
                <div className="space-y-2">
                  {teamMembers.map((member) => (
                    <div
                      key={member.id}
                      className="flex items-center justify-between rounded-lg border border-gray-200 bg-gray-50 p-3 dark:border-gray-700 dark:bg-gray-800"
                    >
                      <div className="flex items-center gap-3">
                        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-accent/10 text-accent">
                          <span className="text-xs font-medium">
                            {member.firstName[0]}
                            {member.lastName[0]}
                          </span>
                        </div>
                        <div>
                          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                            {member.firstName} {member.lastName}
                          </p>
                          <p className="text-xs text-gray-500 dark:text-gray-400">
                            {member.email}
                          </p>
                        </div>
                      </div>
                      <button
                        onClick={() => handleRemoveMember(member.userId)}
                        className="rounded-lg p-1.5 text-gray-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-900/20 dark:hover:text-red-400"
                        title="Retirer"
                      >
                        <UserMinus className="h-4 w-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Available Members */}
            {availableMembers.length > 0 && (
              <div>
                <h4 className="mb-3 text-sm font-semibold text-gray-900 dark:text-gray-100">
                  Ajouter des membres
                </h4>
                <div className="space-y-2">
                  {availableMembers.map((member) => (
                    <div
                      key={member.userId}
                      className="flex items-center justify-between rounded-lg border border-gray-200 p-3 dark:border-gray-700"
                    >
                      <div className="flex items-center gap-3">
                        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300">
                          <span className="text-xs font-medium">
                            {member.firstName[0]}
                            {member.lastName[0]}
                          </span>
                        </div>
                        <div>
                          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                            {member.firstName} {member.lastName}
                          </p>
                          <p className="text-xs text-gray-500 dark:text-gray-400">{member.email}</p>
                        </div>
                      </div>
                      <button
                        onClick={() => handleAddMember(member.userId)}
                        className="rounded-lg p-1.5 text-gray-400 hover:bg-accent/10 hover:text-accent"
                        title="Ajouter"
                      >
                        <UserPlus className="h-4 w-4" />
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </>
        )}

        <Button onClick={onClose} className="w-full">
          Fermer
        </Button>
      </div>
    </Modal>
  );
}
