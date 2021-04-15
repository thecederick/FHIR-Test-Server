package com.example.fhirtest;

import com.ibm.fhir.model.format.Format;
import com.ibm.fhir.model.generator.FHIRGenerator;
import com.ibm.fhir.model.generator.exception.FHIRGeneratorException;
import com.ibm.fhir.model.parser.FHIRParser;
import com.ibm.fhir.model.parser.exception.FHIRParserException;
import com.ibm.fhir.model.resource.OperationOutcome;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.type.CodeableConcept;
import com.ibm.fhir.model.type.code.IssueSeverity;
import com.ibm.fhir.model.type.code.IssueType;
import com.ibm.fhir.validation.FHIRValidator;
import com.ibm.fhir.validation.exception.FHIRValidationException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping
public class ResourceController {

    @RequestMapping(produces = APPLICATION_JSON_VALUE)
    public String endpoint(@RequestBody String body) throws IOException {

        Resource resource = null;
        List<OperationOutcome.Issue> issueList = null;
        try (InputStream in = new ByteArrayInputStream(body.getBytes())) {
            resource = FHIRParser.parser(Format.JSON).parse(in);
            issueList = FHIRValidator.validator()
                    .validate(resource)
                    .stream()
                    .collect(Collectors.toList());
        } catch (FHIRValidationException | FHIRParserException e) {
            issueList = Collections.singletonList(
                    OperationOutcome.Issue.builder()
                            .code(IssueType.INVALID)
                            .severity(IssueSeverity.FATAL)
                            .details(CodeableConcept.builder()
                                    .text(com.ibm.fhir.model.type.String.of(e.getMessage()))
                                    .build())
                            .build());
        }
        try (Writer writer = new StringWriter()) {
            FHIRGenerator.generator(Format.JSON).generate(
                    issueList != null && !issueList.isEmpty() ? OperationOutcome.builder().issue(issueList).build() : resource, writer);
            return writer.toString();
        } catch (IOException | FHIRGeneratorException e) {
            throw new RuntimeException(e);
        }
    }
}
