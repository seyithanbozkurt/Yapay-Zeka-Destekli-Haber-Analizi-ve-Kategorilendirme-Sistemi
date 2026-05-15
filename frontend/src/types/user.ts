export interface UserProfile {
  id: number
  username: string
  email: string
  firstName?: string
  lastName?: string
  birthDate?: string
  active?: boolean
  roles?: string[]
}
