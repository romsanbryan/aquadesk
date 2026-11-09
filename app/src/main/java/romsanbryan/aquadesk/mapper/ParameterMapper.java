package romsanbryan.aquadesk.mapper;


import org.springframework.stereotype.Component;
import romsanbryan.aquadesk.dto.request.ParametersRequest;
import romsanbryan.aquadesk.dto.request.RangeRequest;
import romsanbryan.aquadesk.dto.response.ParametersResponse;
import romsanbryan.data.entities.Parameters;
import romsanbryan.data.entities.ParametersRange;

@Component
public class ParameterMapper {

    public ParametersResponse toParametersSummaryDto(Parameters last, String status) {
        return new ParametersResponse(
                last != null ? last.getNh3() : null,
                last != null ? last.getNo2() : null,
                last != null ? last.getNo3() : null,
                last != null ? last.getPh() : null,
                last != null ? last.getKh() : null,
                last != null ? last.getGh() : null,
                status
        );

    }

    public ParametersRange mapParametersRange(RangeRequest req, ParametersRange existing) {
        existing.setParameter(req.parameter());
        existing.setLevel1(req.level1());
        existing.setLevel2(req.level2());
        existing.setLevel3(req.level3());
        existing.setLevel4(req.level4());
        existing.setLevel5(req.level5());
        existing.setUnit(req.unit());
        return existing;
    }

    public Parameters mapParameters(ParametersRequest req, Integer eventId) {
        Parameters p = new Parameters();
        p.setIdEvent(eventId);
        p.setNh3(req.nh3());
        p.setNo2(req.no2());
        p.setNo3(req.no3());
        p.setPh(req.ph());
        p.setKh(req.kh());
        p.setGh(req.gh());
        p.setTemp(req.temp());

        return p;
    }
}