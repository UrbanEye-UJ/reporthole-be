package za.co.urbaneye.reporthole.security;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Service;

/**
 * Service utility for encrypting and decrypting configuration values using Jasypt.
 *
 * <p>Wraps the application-configured {@link StringEncryptor} bean so that
 * encrypted values can be generated programmatically (e.g. from a CLI runner
 * or an admin endpoint) without duplicating encryptor configuration.</p>
 *
 * <p>Encrypted output from {@link #encrypt(String)} can be placed directly
 * into any {@code application*.yml} file wrapped in the {@code ENC(...)} syntax:
 * <pre>
 *   some.property: ENC(output-from-encrypt-method)
 * </pre>
 * </p>
 *
 * <p>The encryptor algorithm and master password are controlled by the
 * {@code jasypt.encryptor.*} properties defined in {@code application.yaml}.
 * The master password is supplied at runtime via the
 * {@code JASYPT_ENCRYPTOR_PASSWORD} environment variable and must never be
 * committed to source control.</p>
 *
 * @author Refentse
 * @since 1.0
 * @see EnableEncryptableProperties
 */
@Service
public class JasyptEncryptionUtil {

    private final StringEncryptor stringEncryptor;

    /**
     * Constructs the utility with the Spring-managed Jasypt {@link StringEncryptor}.
     *
     * <p>Spring Boot auto-configures this bean when
     * {@code jasypt-spring-boot-starter} is on the classpath and
     * {@code jasypt.encryptor.*} properties are present.</p>
     *
     * @param stringEncryptor the configured Jasypt string encryptor
     */
    public JasyptEncryptionUtil(StringEncryptor stringEncryptor) {
        this.stringEncryptor = stringEncryptor;
    }

    /**
     * Encrypts a plaintext value using the application Jasypt encryptor.
     *
     * <p>The returned string is suitable for wrapping in {@code ENC(...)} and
     * pasting into a Spring configuration file.</p>
     *
     * @param plaintext the value to encrypt; must not be {@code null}
     * @return the Base64-encoded ciphertext produced by the configured encryptor
     * @throws IllegalArgumentException if {@code plaintext} is {@code null}
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext must not be null");
        }
        return stringEncryptor.encrypt(plaintext);
    }

    /**
     * Decrypts a ciphertext value that was previously produced by {@link #encrypt(String)}.
     *
     * <p>The {@code ciphertext} argument should be the raw Base64 string
     * <em>without</em> the surrounding {@code ENC(...)} wrapper.</p>
     *
     * @param ciphertext the Base64-encoded ciphertext to decrypt; must not be {@code null}
     * @return the original plaintext value
     * @throws IllegalArgumentException if {@code ciphertext} is {@code null}
     * @throws org.jasypt.exceptions.EncryptionOperationNotPossibleException if
     *         decryption fails due to a wrong key or corrupted ciphertext
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            throw new IllegalArgumentException("Ciphertext must not be null");
        }
        return stringEncryptor.decrypt(ciphertext);
    }
}
