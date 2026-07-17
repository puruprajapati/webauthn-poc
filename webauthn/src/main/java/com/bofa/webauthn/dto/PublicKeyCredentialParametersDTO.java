package com.bofa.webauthn.dto;

import com.webauthn4j.data.PublicKeyCredentialParameters;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PublicKeyCredentialParametersDTO {
    private final String type;
    private final long alg;

    public static PublicKeyCredentialParametersDTO from(PublicKeyCredentialParameters parameters) {
        return new PublicKeyCredentialParametersDTO(parameters.getType().getValue(), parameters.getAlg().getValue());
    }

//    public PublicKeyCredentialParameters toPublicKeyCredentialParameters() {
//        return new PublicKeyCredentialParameters(
//                com.webauthn4j.data.PublicKeyCredentialType.create(type),
//                com.webauthn4j.data.COSEAlgorithmIdentifier.create(alg)
//        );
//    }
}
