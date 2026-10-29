import { mapBackendProfile } from './backend-profile';
import { microsoftSettings } from '../../../environments/microsoft-settings';
import { UserProfile } from '../models/user-profile.model';

describe('AWS profile compatibility', () => {
  const account = {name: 'Benjamin', username: 'benjaminJerez@pedidos360830.onmicrosoft.com', tenantId: microsoftSettings.tenantId};
  const profile: UserProfile = {status: 'ok', user: 'microsoft-subject', authorities: ['SCOPE_recurso.write', 'ROLE_ADMIN']};
  it('maps the original /api/profile response to the current UI profile', () => {
    const user = mapBackendProfile(profile, account);
    expect(user.isAdmin).toBeTrue();
    expect(user.email).toBe('benjaminjerez@pedidos360830.onmicrosoft.com');
    expect(user.username).toBe('microsoft-subject');
  });
  it('never promotes an approved email without server authorization', () => {
    expect(mapBackendProfile({...profile, authorities: ['ROLE_USER']}, account).isAdmin).toBeFalse();
  });
  it('keeps other emails as USER even if the old server supplies ADMIN', () => {
    const user = mapBackendProfile(profile, {...account, username: 'otro@duocuc.cl'});
    expect(user.isAdmin).toBeFalse();
    expect(user.roles).toEqual(['USER']);
  });
  it('recognizes the two approved guest account names', () => {
    for (const email of ['alva.perezz@duocuc.cl', 'ne.gonzalezm@duocuc.cl']) {
      expect(mapBackendProfile(profile, {...account, username: email.replace('@', '_') + '#EXT#@pedidos360830.onmicrosoft.com'}).isAdmin).toBeTrue();
    }
  });
  it('does not promote an account from another tenant', () => {
    expect(mapBackendProfile(profile, {...account, tenantId: 'another-tenant'}).isAdmin).toBeFalse();
  });
  it('rejects malformed responses instead of inventing a valid session', () => {
    expect(() => mapBackendProfile({} as UserProfile, account)).toThrow();
  });
});
