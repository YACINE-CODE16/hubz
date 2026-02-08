import { useEffect, useState } from 'react';
import { useParams, Outlet } from 'react-router-dom';
import { organizationService } from '../../services/organization.service';
import type { Organization } from '../../types/organization';
import SpaceLayout from '../../components/layout/SpaceLayout';

export default function OrganizationLayout() {
  const { orgId } = useParams<{ orgId: string }>();
  const [org, setOrg] = useState<Organization | null>(null);

  useEffect(() => {
    if (!orgId) return;
    organizationService.getAll().then((orgs) => {
      const found = orgs.find((o) => o.id === orgId);
      if (found) setOrg(found);
    });
  }, [orgId]);

  return (
    <SpaceLayout
      spaceType="organization"
      basePath={`/organization/${orgId}`}
      title={org?.name || 'Organisation'}
      color={org?.color}
      logoUrl={org?.logoUrl}
      icon={org?.icon}
      organizationId={orgId}
    >
      <Outlet />
    </SpaceLayout>
  );
}
