export const BiometricService = {
  isSupported: async (): Promise<boolean> => {
    return !!(window.PublicKeyCredential && await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable());
  },

  authenticate: async (): Promise<boolean> => {
    try {
      const challenge = new Uint8Array([/* real challenge should come from server */ 1, 2, 3]);
      const publicKeyOptions = {
        challenge: challenge,
        userVerification: "required",
      };
      await navigator.credentials.get({ publicKey: publicKeyOptions as any });
      return true;
    } catch (error) {
      console.error("Biometric authentication failed:", error);
      return false;
    }
  }
};
