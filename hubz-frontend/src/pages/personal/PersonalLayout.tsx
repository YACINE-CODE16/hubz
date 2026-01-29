import { Outlet } from 'react-router-dom';
import SpaceLayout from '../../components/layout/SpaceLayout';

export default function PersonalLayout() {
  return (
    <SpaceLayout
      spaceType="personal"
      basePath="/personal"
      title="Mon espace perso"
      color="#6366F1"
    >
      <Outlet />
    </SpaceLayout>
  );
}
