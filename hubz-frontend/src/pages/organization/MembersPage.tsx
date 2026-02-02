import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Trash2, Mail, Copy, Check, Clock, X } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Modal from '../../components/ui/Modal';
import Input from '../../components/ui/Input';
import { organizationService } from '../../services/organization.service';
import { invitationService } from '../../services/invitation.service';
import type { Member } from '../../types/organization';
import type { Invitation, CreateInvitationRequest } from '../../types/invitation';

export default function MembersPage() {
  const { orgId } = useParams<{ orgId: string }>();
  const [members, setMembers] = useState<Member[]>([]);
  const [invitations, setInvitations] = useState<Invitation[]>([]);
  const [loading, setLoading] = useState(true);
  const [isInviteModalOpen, setIsInviteModalOpen] = useState(false);

  useEffect(() => {
    fetchData();
  }, [orgId]);

  const fetchData = async () => {
    if (!orgId) return;
    try {
      setLoading(true);
      const [membersData, invitationsData] = await Promise.all([
        organizationService.getMembers(orgId),
        invitationService.getInvitations(orgId),
      ]);
      setMembers(membersData);
      setInvitations(invitationsData);
    } catch (error) {
      toast.error('Erreur lors du chargement');
    } finally {
      setLoading(false);
    }
  };

  const handleInvite = async (data: CreateInvitationRequest) => {
    if (!orgId) return;
    try {
      await invitationService.createInvitation(orgId, data);
      toast.success('Invitation créée');
      setIsInviteModalOpen(false);
      fetchData();
    } catch (error) {
      toast.error('Erreur lors de la création de l\'invitation');
    }
  };

  const handleDeleteInvitation = async (invitationId: string) => {
    if (!confirm('Supprimer cette invitation ?')) return;
    try {
      await invitationService.deleteInvitation(invitationId);
      toast.success('Invitation supprimée');
      fetchData();
    } catch (error) {
      toast.error('Erreur lors de la suppression');
    }
  };

  const handleCopyLink = (token: string) => {
    const link = invitationService.getInvitationLink(token);
    navigator.clipboard.writeText(link);
    toast.success('Lien copié');
  };

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-gray-500 dark:text-gray-400">Chargement...</div>
      </div>
    );
  }

  const pendingInvitations = invitations.filter((inv) => !inv.used);

  return (
    <div className="flex h-full flex-col gap-6 overflow-auto p-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Membres</h2>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Gérez les membres et invitations de votre organisation
          </p>
        </div>
        <Button onClick={() => setIsInviteModalOpen(true)}>
          <Mail className="h-4 w-4" />
          Inviter un membre
        </Button>
      </div>

      {/* Current Members */}
      <div>
        <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
          Membres actuels ({members.length})
        </h3>
        <div className="grid gap-3">
          {members.map((member) => (
            <MemberCard key={member.userId} member={member} />
          ))}
        </div>
      </div>

      {/* Pending Invitations */}
      {pendingInvitations.length > 0 && (
        <div>
          <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
            Invitations en attente ({pendingInvitations.length})
          </h3>
          <div className="grid gap-3">
            {pendingInvitations.map((invitation) => (
              <InvitationCard
                key={invitation.id}
                invitation={invitation}
                onCopyLink={handleCopyLink}
                onDelete={handleDeleteInvitation}
              />
            ))}
          </div>
        </div>
      )}

      {/* Invite Modal */}
      <InviteModal
        isOpen={isInviteModalOpen}
        onClose={() => setIsInviteModalOpen(false)}
        onInvite={handleInvite}
      />
    </div>
  );
}

interface MemberCardProps {
  member: Member;
}

function MemberCard({ member }: MemberCardProps) {
  const getRoleColor = (role: string) => {
    switch (role) {
      case 'OWNER':
        return 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400';
      case 'ADMIN':
        return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400';
      case 'MEMBER':
        return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400';
      case 'VIEWER':
        return 'bg-gray-100 text-gray-700 dark:bg-gray-900/30 dark:text-gray-400';
      default:
        return 'bg-gray-100 text-gray-700 dark:bg-gray-900/30 dark:text-gray-400';
    }
  };

  const getRoleLabel = (role: string) => {
    switch (role) {
      case 'OWNER':
        return 'Propriétaire';
      case 'ADMIN':
        return 'Administrateur';
      case 'MEMBER':
        return 'Membre';
      case 'VIEWER':
        return 'Lecteur';
      default:
        return role;
    }
  };

  return (
    <Card className="flex items-center justify-between">
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-accent/10 text-accent">
          <span className="text-sm font-medium">
            {member.firstName[0]}
            {member.lastName[0]}
          </span>
        </div>
        <div>
          <p className="font-medium text-gray-900 dark:text-gray-100">
            {member.firstName} {member.lastName}
          </p>
          <p className="text-sm text-gray-500 dark:text-gray-400">{member.email}</p>
        </div>
      </div>
      <span
        className={`rounded-full px-3 py-1 text-xs font-medium ${getRoleColor(member.role)}`}
      >
        {getRoleLabel(member.role)}
      </span>
    </Card>
  );
}

interface InvitationCardProps {
  invitation: Invitation;
  onCopyLink: (token: string) => void;
  onDelete: (id: string) => void;
}

function InvitationCard({ invitation, onCopyLink, onDelete }: InvitationCardProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    onCopyLink(invitation.token);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const isExpired = new Date(invitation.expiresAt) < new Date();

  const getRoleColor = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400';
      case 'MEMBER':
        return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400';
      case 'VIEWER':
        return 'bg-gray-100 text-gray-700 dark:bg-gray-900/30 dark:text-gray-400';
      default:
        return 'bg-gray-100 text-gray-700 dark:bg-gray-900/30 dark:text-gray-400';
    }
  };

  const getRoleLabel = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'Administrateur';
      case 'MEMBER':
        return 'Membre';
      case 'VIEWER':
        return 'Lecteur';
      default:
        return role;
    }
  };

  return (
    <Card className="flex items-center justify-between gap-4">
      <div className="flex flex-1 items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
          <Mail className="h-5 w-5 text-gray-400" />
        </div>
        <div className="flex-1">
          <p className="font-medium text-gray-900 dark:text-gray-100">{invitation.email}</p>
          <div className="mt-1 flex items-center gap-2">
            <span
              className={`rounded-full px-2 py-0.5 text-xs font-medium ${getRoleColor(
                invitation.role
              )}`}
            >
              {getRoleLabel(invitation.role)}
            </span>
            {isExpired ? (
              <span className="flex items-center gap-1 text-xs text-red-600 dark:text-red-400">
                <X className="h-3 w-3" />
                Expirée
              </span>
            ) : (
              <span className="flex items-center gap-1 text-xs text-gray-500 dark:text-gray-400">
                <Clock className="h-3 w-3" />
                Expire le {new Date(invitation.expiresAt).toLocaleDateString('fr-FR')}
              </span>
            )}
          </div>
        </div>
      </div>
      <div className="flex items-center gap-2">
        <Button
          variant="secondary"
          size="sm"
          onClick={handleCopy}
          className="flex items-center gap-1"
        >
          {copied ? (
            <>
              <Check className="h-4 w-4" />
              Copié
            </>
          ) : (
            <>
              <Copy className="h-4 w-4" />
              Copier le lien
            </>
          )}
        </Button>
        <button
          onClick={() => onDelete(invitation.id)}
          className="rounded-lg p-2 text-gray-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-900/20 dark:hover:text-red-400"
          title="Supprimer"
        >
          <Trash2 className="h-4 w-4" />
        </button>
      </div>
    </Card>
  );
}

interface InviteModalProps {
  isOpen: boolean;
  onClose: () => void;
  onInvite: (data: CreateInvitationRequest) => void;
}

function InviteModal({ isOpen, onClose, onInvite }: InviteModalProps) {
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<'ADMIN' | 'MEMBER' | 'VIEWER'>('MEMBER');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) return;

    onInvite({
      email: email.trim(),
      role,
    });

    // Reset form
    setEmail('');
    setRole('MEMBER');
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Inviter un membre">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="exemple@email.com"
          required
        />

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Rôle
          </label>
          <select
            value={role}
            onChange={(e) => setRole(e.target.value as 'ADMIN' | 'MEMBER' | 'VIEWER')}
            className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent dark:border-gray-600 dark:bg-dark-card dark:text-gray-100"
          >
            <option value="VIEWER">Lecteur - Peut uniquement voir</option>
            <option value="MEMBER">Membre - Peut voir et modifier</option>
            <option value="ADMIN">Administrateur - Accès complet</option>
          </select>
          <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
            Le propriétaire peut modifier les rôles plus tard
          </p>
        </div>

        <div className="rounded-lg bg-blue-50 p-3 dark:bg-blue-900/20">
          <p className="text-sm text-blue-900 dark:text-blue-300">
            Un lien d'invitation sera créé. Partagez-le avec la personne pour qu'elle rejoigne
            l'organisation. Le lien expire après 7 jours.
          </p>
        </div>

        <div className="flex gap-2">
          <Button type="button" variant="secondary" onClick={onClose} className="flex-1">
            Annuler
          </Button>
          <Button type="submit" className="flex-1">
            Créer l'invitation
          </Button>
        </div>
      </form>
    </Modal>
  );
}
