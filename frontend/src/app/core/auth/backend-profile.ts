import { AccountInfo } from '@azure/msal-browser';
import { AppUser, UserProfile } from '../models/user-profile.model';
import { microsoftSettings } from '../../../environments/microsoft-settings';

const ADMIN_EMAILS = new Set([
  'benjaminjerez@pedidos360830.onmicrosoft.com',
  'alva.perezz@duocuc.cl',
  'ne.gonzalezm@duocuc.cl',
]);

/** /api/profile is supported by both the original AWS API and the current API. */
export function mapBackendProfile(
  profile: UserProfile,
  account: Pick<AccountInfo, 'name' | 'username' | 'tenantId'>,
): AppUser {
  if (profile?.status !== 'ok' || typeof profile.user !== 'string' || !profile.user
      || !Array.isArray(profile.authorities) || !profile.authorities.every(value => typeof value === 'string')) {
    throw new Error('La API devolvió un perfil de usuario no válido.');
  }
  let email = (account.username || '').trim().toLowerCase();
  for (const approved of ADMIN_EMAILS) {
    if (email === approved.replace('@', '_') + '#ext#@pedidos360830.onmicrosoft.com') email = approved;
  }
  // The server must already authorize ADMIN. A Microsoft claim or an email alone is insufficient.
  const isAdmin = profile.authorities.includes('ROLE_ADMIN')
    && account.tenantId === microsoftSettings.tenantId && ADMIN_EMAILS.has(email);
  return {
    name: account.name || email || profile.user,
    email,
    username: profile.user,
    roles: [isAdmin ? 'ADMIN' : 'USER'],
    isAdmin,
  };
}
